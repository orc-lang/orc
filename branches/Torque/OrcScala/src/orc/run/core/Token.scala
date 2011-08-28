//
// Token.scala -- Scala class/trait/object Token
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.ast.oil.nameless._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.StackLimitReachedError
import orc.error.runtime.TokenException
import orc.error.OrcException
import orc.lib.time.Vawait
import orc.lib.time.Vclock
import orc.lib.time.Vtime
import orc.values.sites.TotalSite
import orc.values.Field
import orc.CaughtEvent
import orc.OrcEvent
import orc.OrcRuntime
import orc.Schedulable
import orc.values.OrcRecord
import orc.values.Signal

/**
 * 
 *
 * @author dkitchin
 */

class Token protected (
  protected var node: Expression,
  protected var stack: List[Frame] = Nil,
  protected var env: List[Binding] = Nil,
  protected var group: Group,
  protected var clock: Option[VirtualClock] = None,
  protected var state: TokenState = Live) 
extends GroupMember with Schedulable {

  var functionFramesPushed: Int = 0
  
  val runtime: OrcRuntime = group.runtime
  val options = group.root.options
  
  /** Execution of a token cannot indefinitely block the executing thread. */
  override val nonblocking = true
  
  /** Public constructor */
  def this(start: Expression, g: Group) = {
    this(node = start, group = g, stack = List(GroupFrame))
  }

  /** Copy constructor with defaults */
  private def copy(
    node: Expression = node,
    stack: List[Frame] = stack,
    env: List[Binding] = env,
    group: Group = group,
    clock: Option[VirtualClock] = clock,
    state: TokenState = state): Token = 
  {
    new Token(node, stack, env, group, clock, state)
  }
  
  
  /*
   * On creation: Add a token to its group if it is not halted or killed.
   */
  state match {
    case Publishing(_) | Live | Blocked(_) | Suspending(_) | Suspended(_) => group.add(this)
    case Halted | Killed => {}
  }
  
  /*
   * On creation: Add a token to its clock if it is not quiescent.
   */
  if (!state.isQuiescent) { clock foreach { _.unsetQuiescent() } }
 
  
  /**
   * Change this token's state.
   * 
   * The state change may trigger activity in the token's clock.
   * If the token's state is already Killed, it remaines Killed,
   * and no clock activity is triggered.
   * 
   * Return true if the token's state was successfully set
   * to the requested state.
   * 
   * Return false if the token's state was already Killed.
   */
  protected def setState(newState: TokenState): Boolean =  {
    /* Record the previous state */
    val oldState = state
    
    /* 
     * Make sure that the token has not been killed.
     * If it has been killed, return false immediately.
     */
    synchronized {
      if (state != Killed) { state = newState } else { return false }
    }
    
    /*
     * Update the clock based on any change in the
     * quiescence of this token.
     */
    (oldState.isQuiescent, newState.isQuiescent) match {
      case (true, false) => clock foreach { _.unsetQuiescent() }
      case (false, true) => clock foreach { _.setQuiescent() }
      case _ => {}
    }
    
    /* The state change did take effect. */
    true
  }
      
  
  /**
   * 
   * Pass an event to this token's enclosing group.
   * 
   * This method is asynchronous:
   * it may be called from a thread other than 
   * the thread currently running this token.
   * 
   */
  def notifyOrc(event: OrcEvent) { group.notifyOrc(event) }


  /**
   * 
   * Kill this token.
   * 
   * This method is asynchronous:
   * it may be called from a thread other than 
   * the thread currently running this token.
   * 
   */
  def kill() {
    def collapseState(victimState: TokenState) {
      victimState match {
        // TODO: Make sure group.halt incurs no danger of deadlock.
        case Live | Publishing(_) => { group.halt(this) }
        case Suspending(s) => collapseState(s)
        case Suspended(s) => collapseState(s)
        case Blocked(handle: SiteCallHandle) => { handle.kill(); group.halt(this) }
        case Blocked(_) => { group.halt(this) }
        case Halted | Killed => {}
      }
    }
    synchronized {
      collapseState(state)
      setState(Killed)
    }
  }

  /**
   * 
   * Make this token block on some resource.
   * 
   * This method is synchronous:
   * it must only be called from a thread that is currently 
   * executing the run() method of this token.
   * 
   */
  def blockOn(blocker: Blocker) {
    state match {
      case Live => setState(Blocked(blocker))
      case Killed => {}
      case _ => throw new AssertionError("Only live tokens may be blocked: state=" + state)
    }
  }

  
  /**
   * 
   * Unblock a token that is currently blocked on some resource.
   * Schedule the token to run.
   * 
   * This method is synchronous:
   * it must only be called from a thread that is currently 
   * executing the run() method of this token.
   * 
   */
  def unblock() {
    state match {
      case Blocked(_) => {
        if (setState(Live)) { schedule() }
      }
      case Suspending(Blocked(_: OtherwiseGroup)) => {
        if (setState(Suspending(Live))) { schedule() }
      }
      case Suspended(Blocked(_: OtherwiseGroup)) => {
        setState(Suspended(Live))
      }
      case Killed => {}
      case _ => { throw new AssertionError("unblock on a Token that is not Blocked/Killed: state=" + state) }
    }
  }

  /**
   * 
   * Suspend the token in preparation for a program rewrite.
   * 
   * This method is asynchronous:
   * it may be called from a thread other than 
   * the thread currently running this token.
   * 
   */
  def suspend() {
    state match {
      case Live | Blocked(_) | Publishing(_) => setState(Suspending(state))
      case Suspending(_) | Suspended(_) | Halted | Killed => {}
    }
  }

  /**
   * 
   * Resume the token from suspension after a program rewrite.
   * 
   * This method is asynchronous:
   * it may be called from a thread other than 
   * the thread currently running this token.
   * 
   */
  def resume() {
    state match {
      case Suspending(prevState) => setState(prevState)
      case Suspended(prevState) => {
        if (setState(prevState)) { schedule() }
      }
      case Publishing(_) | Live | Blocked(_) | Halted | Killed => {}
    }
  }

  def schedule() = runtime.schedule(this)
  
  protected def fork() = synchronized { (this, copy()) }

  def move(e: Expression) = { node = e; this }

  def jump(context: List[Binding]) = { env = context; this }
  
  protected def push(f: Frame) = { stack = f :: stack; this }

  def getGroup(): Group = { group }
  def getNode(): Expression = { node }
  def getEnv(): List[Binding] = { env }
  def getStack(): List[Frame] = { stack }
  
  def migrate(newGroup: Group) = {
    val oldGroup = group
    newGroup.add(this); oldGroup.remove(this)
    group = newGroup
    this
  }

  protected def join(newGroup: Group) = { 
    push(GroupFrame)
    migrate(newGroup) 
    this
  }

  
  
  
  
  def bind(b: Binding) = {
    env = b :: env
    stack match {
      case BindingFrame(n) :: fs => { stack = (new BindingFrame(n + 1)) :: fs }

      /* Tail call optimization (part 1 of 2) */
      case FunctionFrame(_, _) :: fs if (!options.disableTailCallOpt) => { /* Do not push a binding frame over a tail call.*/ }

      case fs => { stack = BindingFrame(1) :: fs }
    }
    this
  }
  
  def unbind(n: Int) = { env = env.drop(n); this }

  protected def lookup(a: Argument): Binding = {
    a match {
      case Constant(v) => BoundValue(v)
      case Variable(n) => env(n)
      case UnboundVariable(x) => BoundStop //TODO: Also report the presence of an unbound variable.
    }
  }

  /**
   * Attempt to resolve a binding to a value.
   * When the binding resolves to v, call k(v).
   * (If it is already resolved, k is called immediately)
   *
   * If the binding resolves to a halt, halt this token.
   */
  protected def resolve(b: Binding)(k: AnyRef => Unit) {
    resolveOptional(b) {
      case Some(v) => k(v)
      case None => halt()
    }
  }

  /**
   * Attempt to resolve a binding to a value.
   * When the binding resolves to v, call k(Some(v)).
   * (If it is already resolved, k is called immediately)
   *
   * If the binding resolves to a halt, call k(None).
   *
   * Note that resolving a closure also encloses its context.
   */
  protected def resolveOptional(b: Binding)(k: Option[AnyRef] => Unit): Unit = {
    b match {
      case BoundValue(v) =>
        v match {
          case c: Closure =>
            enclose(c.lexicalContext) { newContext: List[Binding] =>
              k(Some(Closure(c.defs, c.pos, newContext)))
            }
          case u => k(Some(u))
        }
      case BoundStop => k(None)
      case BoundFuture(g) => {
        stack = FutureFrame(k) :: stack
        g read this
      }
    }
  }

  /**
   * Create a new Closure object whose lexical bindings are all resolved and replaced.
   * Such a closure will have no references to any group.
   * This object is then passed to the continuation.
   */
  protected def enclose(bs: List[Binding])(k: List[Binding] => Unit): Unit = {
    def resolveBound(b: Binding)(k: Binding => Unit) =
      resolveOptional(b) {
        case Some(v) => k(BoundValue(v))
        case None => k(BoundStop)
      }
    leftToRight(resolveBound, bs)(k)
  }

  
  
  
  protected def functionCall(d: Def, context: List[Binding], params: List[Binding]) {
    if (params.size != d.arity) {
      this !! new ArityMismatchException(d.arity, params.size) /* Arity mismatch. */
    } else {

      /* 1) If this is not a tail call, push a function frame referring to the current environment.
       * 2) Change the current environment to the closure's saved environment.
       * 3) Add bindings for the arguments to the new current environment.
       *
       * Caution: The ordering of these operations is very important;
       *          do not permute them.
       */

      /* Tail call optimization (part 2 of 2) */
      stack match {
        /*
         * Push a new FunctionFrame
         * only if the call is not a tail call.
         */
        case FunctionFrame(_, _) :: fs if (!options.disableTailCallOpt) => {}
        case _ => {
          functionFramesPushed = functionFramesPushed + 1
          if (options.stackSize > 0 &&
            functionFramesPushed > options.stackSize)
            throw new StackLimitReachedError(options.stackSize)
          push(new FunctionFrame(node, env))
        }
      }

      /* Jump into the function context */
      jump(context)

      /* Bind the args */
      for (p <- params) { bind(p) }

      /* Jump into the function body */
      move(d.body)
      schedule()
    }
  }

  
  protected def clockCall(vc: VirtualClockOperation, actuals: List[AnyRef]): Unit = {
    (vc, actuals) match {
      case (`Vclock`,List(f)) => {
        f match {
          case totalf: TotalSite => { 
            def ordering(x: AnyRef, y: AnyRef) = {
              // TODO: Add error handling, either here or in the scheduler.
              // A comparator error should kill the engine.
              val i = totalf.evaluate(List(x,y)).asInstanceOf[Int]
              assert(i == -1 || i == 0 || i == 1)
              i
            }
            clock = Some(new VirtualClock(clock, ordering, runtime))
            publish()
          }
          case _ => {
            this !! (new ArgumentTypeMismatchException(0, "TotalSite", f.toString()))
          }
        }
      }
      case (`Vawait`,List(t)) => {
        clock match {
          case Some(cl) => cl.await(this, t)
          case None => halt()
        }
      }
      case (`Vtime`,Nil) => {
        clock flatMap { _.now() } match {
          case Some(t) => publish(t)
          case None => halt()
        }
      }
      case _ => throw new ArityMismatchException(vc.arity, actuals.size)
    }
  }
  
  protected def siteCall(s: AnyRef, actuals: List[AnyRef]): Unit = {
    s match {
      case vc: VirtualClockOperation => {
        clockCall(vc, actuals)
      }
      case _ => {
        val sh = new SiteCallHandle(this, s, actuals)
        blockOn(sh)
        runtime.schedule(sh)
      }
    }
  }

  protected def makeCall(target: AnyRef, params: List[Binding]): Unit = {
    target match {
      case c: Closure => {
        functionCall(c.code, c.context, params)
      }

      /* I wish this didn't need a special case... 
       * but if the record element is a closure,
       * it can't be handled by an invocation trait.
       * -dkitchin
       */
      case r @ OrcRecord(entries) if entries contains "apply" => {
        leftToRight(resolve, params) {
          case args @ List(Field(_)) => siteCall(r, args) // apply isn't allowed to supersede other member accesses
          case _ => makeCall(entries("apply"), params)
        }
      }

      case s => {
        leftToRight(resolve, params) { siteCall(s, _) }
      }
    }
  }

  
  
  
  def run() {
    if (group.isKilled()) { kill() }
    state match {
      case Live => eval(node)
      case Suspending(prevState) => setState(Suspended(prevState))      
      case Blocked(b) => b.check(this)
      
      // TODO: Remove this state entirely to reduce scheduler pummeling
      //       and more faithfully replicate semantics.
      case Publishing(v) => {
        if (setState(Live)) {
          stack match {
            case f :: fs => {
              stack = fs
              if (f.isInstanceOf[FunctionFrame]) {
                functionFramesPushed = functionFramesPushed - 1
              }
              f(this, v)
            }
            case Nil => {
              throw new AssertionError("publish on an empty stack")
            }
          }
        }
      }
      
      case Killed => {} // This token was killed while it was on the schedule queue; ignore it
      
      case Suspended(_) => throw new AssertionError("suspended token scheduled")
      case Halted => throw new AssertionError("halted token scheduled")
    }
  }

  protected def eval(node: orc.ast.oil.nameless.Expression) {
    node match {
      case Stop() => halt()

      case Hole(_, _) => halt()

      case (a: Argument) => resolve(lookup(a)) { publish }

      case Call(target, args, _) => {
        val params = args map lookup
        lookup(target) match {
          case BoundValue(c: Closure) => functionCall(c.code, c.context, params)
          case b => resolve(b) { makeCall(_, params) }
        }
      }

      case Parallel(left, right) => {
        val (l, r) = fork()
        l.move(left)
        r.move(right)
        runtime.schedule(l, r)
      }

      case Sequence(left, right) => {
        val frame = new SequenceFrame(right)
        push(frame)
        move(left)
        schedule()
      }

      case Prune(left, right) => {
        val (l, r) = fork
        val pg = new PruningGroup(group)
        l.bind(BoundFuture(pg))
        r.join(pg)
        l.move(left)
        r.move(right)
        runtime.schedule(l, r)
      }

      case Otherwise(left, right) => {
        val (l, r) = fork
        r.move(right)
        val region = new OtherwiseGroup(group, r)
        l.join(region)
        l.move(left)
        runtime.schedule(l)
      }

      case decldefs @ DeclareDefs(openvars, defs, body) => {
        /* Closure compaction: Bind only the free variables
         * of the defs in this lexical context.
         */
        val lexicalContext = openvars map { i: Int => lookup(Variable(i)) }
        for (i <- defs.indices) {
          bind(BoundValue(Closure(defs, i, lexicalContext)))
        }
        move(body)
        schedule()
      }

      case HasType(expr, _) => {
        move(expr)
        run()
      }

      case DeclareType(_, expr) => {
        move(expr)
        run()
      }
    }
  }


  def publish(v: AnyRef) {
    state match {
      case Blocked(_: OtherwiseGroup) => throw new AssertionError("publish on a pending Token")
      case Live | Blocked(_) => {
        setState(Publishing(v))
        schedule()
      }
      case Suspending(_) => {
        setState(Suspending(Publishing(v)))
        schedule()
      }
      case Suspended(_) => {
        setState(Suspended(Publishing(v)))
      }
      case Publishing(_) => throw new AssertionError("Already publishing!")
      case Halted | Killed => {}
    }
  }
  
  def publish() { publish(Signal) }

  def halt() {
    state match {
      case Publishing(_) | Live | Blocked(_) | Suspending(_) => {
        setState(Halted)
        group.halt(this)
      }
      case Suspended(_) => throw new AssertionError("halt on a suspended Token")
      case Halted | Killed => {}
    }
  }

  def !!(e: OrcException) {
    e.setPosition(node.pos)
    e match {
      case te: TokenException if (te.getBacktrace() == null || te.getBacktrace().length == 0) => {
        val callPoints = stack collect { case f: FunctionFrame => f.callpoint.pos }
        te.setBacktrace(callPoints.toArray)
      }
      case _ => {} // Not a TokenException; no need to collect backtrace
    }
    notifyOrc(CaughtEvent(e))
    halt()
  }

  /** Utility function for chaining a continuation across a list */
  protected def leftToRight[X, Y](f: X => (Y => Unit) => Unit, xs: List[X])(k: List[Y] => Unit): Unit = {
    def walk(xs: List[X], ys: List[Y]): Unit = {
      xs match {
        case z :: zs => f(z) { y: Y => walk(zs, y :: ys) }
        case Nil => k(ys.reverse)
      }
    }
    walk(xs, Nil)
  }

}