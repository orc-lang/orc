//
// Token.scala -- Scala class Token
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.{ Schedulable, OrcRuntime, OrcEvent, CaughtEvent }
import orc.ast.oil.nameless.{ Variable, UnboundVariable, Stop, Sequence, Prune, Parallel, Otherwise, Hole, HasType, Expression, Def, DeclareType, DeclareDefs, Constant, Call, Argument }
import orc.error.runtime.{ TokenException, StackLimitReachedError, ArityMismatchException, ArgumentTypeMismatchException }
import orc.error.OrcException
import orc.lib.time.{ Vtime, Vclock, Vawait }
import orc.util.BlockableMapExtension.addBlockableMapToList
import orc.values.sites.TotalSite
import orc.values.{ Signal, OrcRecord, Field }
import orc.ast.oil.nameless.VtimeZone
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Token represents a "process" executing in the Orc program.
  *
  * For lack of a better place to put it here is a little
  * documentation of how publish and other methods on Token
  * and other classes (notably Group) handle their Option[AnyRef]
  * argument. None represents stop and Some(v) represents the
  * value v. So the expression x.get represents the assumption
  * (runtime checked) that x is not a stop value.
  *
  * This convention is not consistent as the Java sites are not
  * able to access the Option type well, so the Site API is
  * unchanged. This confines the changes to the core runtime,
  * however it does mean that there are still parts of the code
  * where there is no way to represent stop.
  *
  * Be careful when blocking because you may be unblocked
  * immediately in a different thread.
  *
  * @see Blockable
  *
  * @author dkitchin
  */
class Token protected (
  protected var node: Expression,
  protected var stack: Frame = EmptyFrame,
  protected var env: List[Binding] = Nil,
  protected var group: Group,
  protected var clock: Option[VirtualClock] = None,
  protected var state: TokenState = Live)
  extends GroupMember with Schedulable with Blockable with Resolver {

  var functionFramesPushed: Int = 0

  val runtime: OrcRuntime = group.runtime

  def sourcePosition = node.pos

  val options = group.root.options

  /** Execution of a token cannot indefinitely block the executing thread. */
  override val nonblocking = true

  /** Public constructor */
  def this(start: Expression, g: Group) = {
    this(node = start, group = g, stack = GroupFrame(EmptyFrame))
  }

  /** Copy constructor with defaults */
  private def copy(
    node: Expression = node,
    stack: Frame = stack,
    env: List[Binding] = env,
    group: Group = group,
    clock: Option[VirtualClock] = clock,
    state: TokenState = state): Token = {
    new Token(node, stack, env, group, clock, state)
  }

  /*
   * On creation: Add a token to its group if it is not halted or killed.
   * 
   * All initialization that must occure before run() executes must happen 
   * before this point, because once the token is added to a group it may 
   * run at any time.
   * 
   */
  state match {
    case Publishing(_) | Live | Blocked(_) | Suspending(_) | Suspended(_) => group.add(this)
    case Halted | Killed => {}
  }

  private val toStringRecusionGuard = new ThreadLocal[Object]()
  override def toString = {
    try {
      val recursing = toStringRecusionGuard.get
      toStringRecusionGuard.set(java.lang.Boolean.TRUE)
      super.toString + (if (recursing eq null) s"(state=$state, node=$node, group=$group, clock=$clock)" else "")
    } finally {
      toStringRecusionGuard.remove()
    }
  }

  /** Change this token's state.
    *
    * Return true if the token's state was successfully set
    * to the requested state.
    *
    * Return false if the token's state was already Killed.
    */
  protected def setState(newState: TokenState): Boolean = synchronized {
    /*
     * Make sure that the token has not been killed.
     * If it has been killed, return false immediately.
     */
    if (state != Killed) { state = newState; true } else false
  }

  /** An expensive walk-to-root check for alive state */
  def checkAlive(): Boolean = state.isLive && group.checkAlive()

  def setQuiescent() { clock foreach { _.setQuiescent() } }

  def unsetQuiescent() { clock foreach { _.unsetQuiescent() } }
  
  /* When a token is scheduled, notify its clock accordingly */
  override def onSchedule() {
    unsetQuiescent()
  }

  /* When a token is finished running, notify its clock accordingly */
  override def onComplete() {
    setQuiescent()
  }

  /** Pass an event to this token's enclosing group.
    *
    * This method is asynchronous:
    * it may be called from a thread other than
    * the thread currently running this token.
    */
  def notifyOrc(event: OrcEvent) { group.notifyOrc(event) }

  /** Kill this token.
    *
    * This method is asynchronous:
    * it may be called from a thread other than
    * the thread currently running this token.
    */
  def kill() {
    def findHandle(victimState: TokenState): Option[CallHandle] = {
      victimState match {
        case Suspending(s) => findHandle(s)
        case Suspended(s) => findHandle(s)
        case Blocked(handle: SiteCallHandle) => Some(handle)
        case Live | Publishing(_) | Blocked(_) | Halted | Killed => None
      }
    }
    synchronized {
      val handle = findHandle(state)
      if (setState(Killed)) {
        /* group.remove(this) conceptually runs here, but as an optimization,
         * this is unnecessary. Note that the current Group.remove implementation
         * relies on this optimization for correctness of the tokenCount. */
      }
      handle foreach { _.kill }
    }
  }

  /** Make this token block on some resource.
    *
    * This method is synchronous:
    * it must only be called from a thread that is currently
    * executing the run() method of this token.
    */
  def blockOn(blocker: Blocker) {
    state match {
      case Live => setState(Blocked(blocker))
      case Killed => {}
      case _ => throw new AssertionError("Only live tokens may be blocked: state=" + state)
    }
  }

  /** Unblock a token that is currently blocked on some resource.
    * Schedule the token to run.
    *
    * This method is synchronous:
    * it must only be called from a thread that is currently
    * executing the run() method of this token.
    */
  def unblock() {
    state match {
      case Blocked(_) => {
        if (setState(Live)) { runtime.stage(this) }
      }
      case Suspending(Blocked(_: OtherwiseGroup)) => {
        if (setState(Suspending(Live))) { runtime.stage(this) }
      }
      case Suspended(Blocked(_: OtherwiseGroup)) => {
        setState(Suspended(Live))
      }
      case Killed => {}
      case _ => { throw new AssertionError("unblock on a Token that is not Blocked/Killed: state=" + state) }
    }
  }

  /** Suspend the token in preparation for a program rewrite.
    *
    * This method is asynchronous:
    * it may be called from a thread other than
    * the thread currently running this token.
    */
  def suspend() {
    state match {
      case Live | Blocked(_) | Publishing(_) => setState(Suspending(state))
      case Suspending(_) | Suspended(_) | Halted | Killed => {}
    }
  }

  /** Resume the token from suspension after a program rewrite.
    *
    * This method is asynchronous:
    * it may be called from a thread other than
    * the thread currently running this token.
    */
  def resume() {
    state match {
      case Suspending(prevState) => setState(prevState)
      case Suspended(prevState) => {
        if (setState(prevState)) { runtime.stage(this) }
      }
      case Publishing(_) | Live | Blocked(_) | Halted | Killed => {}
    }
  }

  protected def fork() = synchronized { (this, copy()) }

  def move(e: Expression) = { node = e; this }

  def jump(context: List[Binding]) = { env = context; this }

  protected def push(newStack: Frame) = {
    if (newStack.isInstanceOf[FunctionFrame]) {
      functionFramesPushed = functionFramesPushed + 1
      if (options.stackSize > 0 && functionFramesPushed > options.stackSize) {
        this !! new StackLimitReachedError(options.stackSize)
      }
    }
    stack = newStack
    this
  }

  protected def pushContinuation(k: (Option[AnyRef] => Unit)) = push(FutureFrame(k, stack))

  /** Remove the top frame of this token's stack.
    *
    * This method is synchronous:
    * it must only be called from a thread that is currently
    * executing the run() method of this token.
    */
  def pop() = {
    if (stack.isInstanceOf[FunctionFrame]) {
      functionFramesPushed = functionFramesPushed - 1
    }
    stack = stack.asInstanceOf[CompositeFrame].previous
  }

  def getGroup(): Group = { group }
  def getNode(): Expression = { node }
  def getEnv(): List[Binding] = { env }
  def getStack(): Frame = { stack }
  def getClock(): Option[VirtualClock] = { clock }

  def migrate(newGroup: Group) = {
    require(newGroup != group)
    val oldGroup = group
    newGroup.add(this); oldGroup.remove(this)
    group = newGroup
    this
  }

  protected def join(newGroup: Group) = {
    push(GroupFrame(stack))
    migrate(newGroup)
    this
  }

  def bind(b: Binding) = {
    env = b :: env
    stack match {
      case BindingFrame(n, previous) => { stack = BindingFrame(n + 1, previous) }

      /* Tail call optimization (part 1 of 2) */
      case _: FunctionFrame if (!options.disableTailCallOpt) => { /* Do not push a binding frame over a tail call.*/ }

      case _ => { push(BindingFrame(1, stack)) }
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
      /*
       * Push a new FunctionFrame
       * only if the call is not a tail call.
       */
      if (!stack.isInstanceOf[FunctionFrame] || options.disableTailCallOpt) {
        push(FunctionFrame(node, env, stack))
      }

      /* Jump into the function context */
      jump(context)

      /* Bind the args */
      for (p <- params) { bind(p) }

      /* Move into the function body */
      move(d.body)
      runtime.stage(this)
    }
  }

  protected def clockCall(vc: VirtualClockOperation, actuals: List[AnyRef]) {
    (vc, actuals) match {
      case (`Vawait`, List(t)) => {
        clock match {
          case Some(cl) => cl.await(this, t)
          case None => halt()
        }
      }
      case (`Vtime`, Nil) => {
        clock flatMap { _.now() } match {
          case Some(t) => publish(Some(t))
          case None => halt()
        }
      }
      case _ => this !! new ArityMismatchException(vc.arity, actuals.size)
    }
  }

  protected def siteCall(s: AnyRef, actuals: List[AnyRef]) {
    s match {
      case vc: VirtualClockOperation => {
        clockCall(vc, actuals)
      }
      case _ => {
        val sh = new SiteCallHandle(this, s, actuals)
        blockOn(sh)
        runtime.stage(sh)
      }
    }
  }

  /** Make a call.
    * The call target is resolved, but the parameters are not yet resolved.
    */
  protected def makeCall(target: AnyRef, params: List[Binding]) {
    target match {
      case c: Closure => {
        functionCall(c.code, c.context, params)
      }
      case _ => {
        params match {
          /* Zero parameters. No need to block. */
          case Nil => {
            target match {
              case r @ OrcRecord(entries) if entries contains "apply" => makeCall(entries("apply"), params)
              case s => siteCall(s, Nil)
            }
          }

          /* One parameter. May need to block. No need to join. */
          case List(param) => {
            target match {
              case r @ OrcRecord(entries) if entries contains "apply" => {
                resolveOptional(param) {
                  /* apply isn't allowed to supersede other member accesses */
                  case Some(Field(f)) => siteCall(r, List(Field(f)))
                  /*
                   * The resolved arg is ignored and the apply member is retried on the parameters.
                   * The arg is ignored even if it is halted, since the apply member might be a function.
                   */
                  case _ => makeCall(entries("apply"), params)
                }
              }
              case s => resolve(param) { arg: AnyRef => siteCall(s, List(arg)) }
            }
          }

          /* Multiple parameters. May need to join. */
          case _ => {
            target match {
              case r @ OrcRecord(entries) if entries contains "apply" => makeCall(entries("apply"), params)
              case s => {

                /* Prepare to receive a list of arguments from the join once all parameters are resolved. */
                pushContinuation({
                  case Some(args: List[_]) => siteCall(s, args.asInstanceOf[List[AnyRef]])
                  case Some(_) => throw new AssertionError("Join resulted in a non-list")
                  case None => halt()
                })

                /* Create a join over the parameters. */
                val j = new Join(params, this, runtime)

                /* Perform the join. */
                j.join()
              }
            }
          }
        }
      }
    }
  }

  def designateClock(newClock: Option[VirtualClock]) {
    newClock foreach { _.unsetQuiescent() }
    clock foreach { _.setQuiescent() }
    clock = newClock
  }

  def newVclock(orderingArg: AnyRef, body: Expression) = {
    orderingArg match {
      case orderingSite: TotalSite => {
        def ordering(x: AnyRef, y: AnyRef) = {
          // TODO: Add error handling, either here or in the scheduler.
          // A comparator error should kill the engine.
          val i = orderingSite.evaluate(List(x, y)).asInstanceOf[Int]
          assert(i == -1 || i == 0 || i == 1, "Vclock time comparator " + orderingSite.name + " did not return -1/0/1")
          i
        }
        join(new VirtualClockGroup(clock, group))
        designateClock(Some(new VirtualClock(ordering, runtime)))
        move(body)
        runtime.stage(this)
      }
      case _ => {
        this !! (new ArgumentTypeMismatchException(0, "TotalSite", orderingArg.toString()))
      }
    }
  }

  //def stackOK(testStack: Array[java.lang.StackTraceElement], offset: Int): Boolean =
  //  testStack.length == 4 + offset && testStack(1 + offset).getMethodName() == "runTask" ||
  //    testStack(1 + offset).getMethodName() == "eval" && testStack(2 + offset).getMethodName() == "run" && stackOK(testStack, offset + 2)

  def run() {
    //val ourStack = new Throwable("Entering Token.run").getStackTrace()
    //assert(stackOK(ourStack, 0), "Token run not in ThreadPoolExecutor.Worker! sl="+ourStack.length+", m1="+ourStack(1).getMethodName()+", state="+state)
    try {
      if (group.isKilled()) { kill() }
      state match {
        case Live => eval(node)
        case Suspending(prevState) => setState(Suspended(prevState))
        case Blocked(b) => b.check(this)
        case Publishing(v) => if (setState(Live)) { stack(this, v) }
        case Killed => {} // This token was killed while it was on the schedule queue; ignore it
        case Suspended(_) => throw new AssertionError("suspended token scheduled")
        case Halted => throw new AssertionError("halted token scheduled")
      }
    } catch {
      case e: OrcException => this !! e
      case e: InterruptedException => { halt(); Thread.currentThread().interrupt() } //Thread interrupt causes halt without notify
      case e: Throwable => { notifyOrc(CaughtEvent(e)); halt() }
    }
  }

  protected def eval(node: orc.ast.oil.nameless.Expression) {
    node match {
      case Stop() => halt()

      case Hole(_, _) => halt()

      case (a: Argument) => resolve(lookup(a)) { v => publish(Some(v)) }

      case Call(target, args, _) => {
        val params = args map lookup
        lookup(target) match {
          /*
           * Allow a def to be called with an open context.
           * This functionality is sound, but technically exceeds the formal semantics of Orc.
           */
          case BoundClosure(c: Closure) => functionCall(c.code, c.context, params)

          case b => resolve(b) { makeCall(_, params) }
        }
      }

      case Parallel(left, right) => {
        val (l, r) = fork()
        l.move(left)
        r.move(right)
        runtime.stage(l, r)
      }

      case Sequence(left, right) => {
        push(SequenceFrame(right, stack))
        move(left)
        runtime.stage(this)
      }

      case Prune(left, right) => {
        val (l, r) = fork()
        val pg = new PruningGroup(group)
        l.bind(BoundFuture(pg))
        r.join(pg)
        l.move(left)
        r.move(right)
        runtime.stage(l, r)
      }

      case Otherwise(left, right) => {
        val (l, r) = fork
        r.move(right)
        val region = new OtherwiseGroup(group, r)
        l.join(region)
        l.move(left)
        runtime.stage(l)
      }

      case VtimeZone(timeOrdering, body) => {
        resolve(lookup(timeOrdering)) { newVclock(_, body) }
      }

      case decldefs @ DeclareDefs(openvars, defs, body) => {
        /* Closure compaction: Bind only the free variables
         * of the defs in this lexical context.
         */
        val lexicalContext = openvars map { i: Int => lookup(Variable(i)) }
        for (i <- defs.indices) {
          val c = new Closure(defs, i, lexicalContext, runtime)
          runtime.stage(c)
          bind(BoundClosure(c))
        }
        move(body)
        runtime.stage(this)
      }

      case HasType(expr, _) => {
        move(expr)
        eval(this.node)
      }

      case DeclareType(_, expr) => {
        move(expr)
        eval(this.node)
      }
    }
  }

  def publish(v: Option[AnyRef]) {
    state match {
      case Blocked(_: OtherwiseGroup) => throw new AssertionError("publish on a pending Token")
      case Live | Blocked(_) => {
        setState(Publishing(v))
        runtime.stage(this)
      }
      case Suspending(_) => {
        setState(Suspending(Publishing(v)))
        runtime.stage(this)
      }
      case Suspended(_) => {
        setState(Suspended(Publishing(v)))
      }
      case Publishing(_) => throw new AssertionError("Already publishing!")
      case Halted | Killed => {}
    }
  }

  def publish() { publish(Some(Signal)) }

  override def halt() {
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
        val callPoints = stack.toList collect { case f: FunctionFrame => f.callpoint.pos }
        te.setBacktrace(callPoints.toArray)
      }
      case _ => {} // Not a TokenException; no need to collect backtrace
    }
    notifyOrc(CaughtEvent(e))
    halt()
  }

  def awakeValue(v: AnyRef) = publish(Some(v))
  def awakeStop() = publish(None)

  override def awakeException(e: OrcException) = this !! e

  override def awake() { unblock() }
}

/**  */
trait TokenState {
  val isLive: Boolean
}

/** Token is ready to make progress */
case object Live extends TokenState {
  val isLive = true
}

/** Token is propagating a published value */
case class Publishing(v: Option[AnyRef]) extends TokenState {
  val isLive = true
}

/** Token is waiting on another task */
case class Blocked(blocker: Blocker) extends TokenState {
  val isLive = true
}

/** Token has been told to suspend, but it's still in the scheduler queue */
case class Suspending(prevState: TokenState) extends TokenState {
  val isLive = prevState.isLive
}

/** Suspended Tokens must be re-scheduled upon resume */
case class Suspended(prevState: TokenState) extends TokenState {
  val isLive = prevState.isLive
}

/** Token halted itself */
case object Halted extends TokenState {
  val isLive = false
}

/** Token killed by engine */
case object Killed extends TokenState {
  val isLive = false
}
