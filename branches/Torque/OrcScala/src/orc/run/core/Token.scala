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
      state: TokenState = state): Token = {

      new Token(node, stack, env, group, clock, state)
    }
    
    // A live token is added to its group when it is created
    state match {
      case Published(_) | Live | Blocked(_) | Suspending(_) | Suspended(_) => group.add(this)
      case Halted | Killed => {}
    }
    
    // A nonquiescent token is added to its clock when it is created
    if (!state.isQuiescent) { clock foreach { _.unsetQuiescent() } }
    
    
    def options = group.root.options
    
    def setState(newState: TokenState)  {
      val (oldState, oldClock) = synchronized {
        val oldState = state
        state = newState
        (oldState, clock)
      }
      (oldState.isQuiescent, newState.isQuiescent) match {
        case (true, false) => oldClock foreach { _.unsetQuiescent() }
        case (false, true) => oldClock foreach { _.setQuiescent() }
        case _ => {}
      }
    }
        
    def notifyOrc(event: OrcEvent) { group.notifyOrc(event) }

    def kill() = synchronized {
      state match {
        case Published(_) | Live | Blocked(_) | Suspending(_) | Suspended(_) => {
          state match {
            case Blocked(handle: SiteCallHandle) => handle.kill()
            case _ => {}
          }
          setState(Killed)
          group.halt(this)
        }
        case Halted | Killed => {}
      }
    }

    def blockOn(blocker: Blocker) = synchronized {
      state match {
        case Live => setState(Blocked(blocker))
        case Killed => {}
        case _ => throw new AssertionError("Only live tokens may be blocked: state=" + state)
      }
    }

    def unblock() = synchronized {
      state match {
        case Blocked(_: OtherwiseGroup) => {
          setState(Live)
          schedule()
        }
        case Killed => {}
        case Suspending(Blocked(_: OtherwiseGroup)) => {
          setState(Suspending(Live))
          schedule()
        }
        case Suspended(Blocked(_: OtherwiseGroup)) => {
          setState(Suspended(Live))
        }
        case Blocked(_) => { throw new AssertionError("Tokens may only receive _.unblock from a region") }
        case _ => { throw new AssertionError("unblock on a Token that is not Blocked/Killed: state=" + state) }
      }
    }

    def suspend() = synchronized {
      state match {
        case Live | Blocked(_) | Published(_) => setState(Suspending(state))
        case Suspending(_) | Suspended(_) | Halted | Killed => {}
      }
    }

    def resume() = synchronized {
      state match {
        case Suspending(prevState) => setState(prevState)
        case Suspended(prevState) => {
          setState(prevState)
          schedule()
        }
        case Published(_) | Live | Blocked(_) | Halted | Killed => {}
      }
    }

    def schedule() = runtime.schedule(this)
    
    def fork() = { (this, copy()) }

    def move(e: Expression) = { node = e; this }

    def jump(context: List[Binding]) = { env = context; this }
    
    def push(f: Frame) = { stack = f :: stack; this }

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

    def join(newGroup: Group) = { 
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

    def lookup(a: Argument): Binding = {
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
    def resolve(b: Binding)(k: AnyRef => Unit) {
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
    def resolveOptional(b: Binding)(k: Option[AnyRef] => Unit): Unit = {
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
          stack = PruningFrame(k) :: stack
          g read this
        }
      }
    }

    /**
     * Create a new Closure object whose lexical bindings are all resolved and replaced.
     * Such a closure will have no references to any group.
     * This object is then passed to the continuation.
     */
    def enclose(bs: List[Binding])(k: List[Binding] => Unit): Unit = {
      def resolveBound(b: Binding)(k: Binding => Unit) =
        resolveOptional(b) {
          case Some(v) => k(BoundValue(v))
          case None => k(BoundStop)
        }
      leftToRight(resolveBound, bs)(k)
    }

    def functionCall(d: Def, context: List[Binding], params: List[Binding]) {
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

    def siteCall(s: AnyRef, actuals: List[AnyRef]): Unit = {
      val sh = new SiteCallHandle(this, s, actuals)
      setState(Blocked(sh))
      (s, actuals) match {
        // Arity errors for these calls are caught in invoke(...)
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
              synchronized { clock = Some(new VirtualClock(clock, ordering)) }
              sh.publish()
            }
            case _ => {
              sh !! (new ArgumentTypeMismatchException(0, "TotalSite", f.toString()))
            }
          }
        }
        case (`Vawait`,List(t)) => {
          clock match {
            case Some(cl) => {
              if (cl.await(sh, t)) {
                cl.setQuiescent()
              }
            }
            case None => sh.halt
          }
        }
        case (`Vtime`,Nil) => {
          clock flatMap { _.now() } match {
            case Some(t) => sh.publish(t)
            case None => sh.halt
          }
        }
        case (_,_) => {
          runtime.schedule(sh)
        }
      }
    }

    def makeCall(target: AnyRef, params: List[Binding]): Unit = {
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
      var runNode = false
      synchronized {
        state match {
          case Live => runNode = true // Run this token's current AST node, ouside this synchronized block
          case Blocked(_) => throw new AssertionError("blocked token scheduled")
          case Suspending(prevState) => setState(Suspended(prevState))
          case Suspended(_) => throw new AssertionError("suspended token scheduled")
          case Halted => throw new AssertionError("halted token scheduled")
          case Killed => {} // This token was killed while it was on the schedule queue; ignore it
          case Published(v) => {
            setState(Live)
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
      }
      if (runNode) eval(node)
    }

    def eval(node: orc.ast.oil.nameless.Expression) {
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

    // Publicly accessible methods

    def publish(v: AnyRef) {
      state match {
        case Blocked(_: OtherwiseGroup) => throw new AssertionError("publish on a pending Token")
        case Live | Blocked(_) => {
          setState(Published(v))
          schedule()
        }
        case Suspending(_) => {
          setState(Suspending(Published(v)))
          schedule()
        }
        case Suspended(_) => {
          setState(Suspended(Published(v)))
        }
        case Published(_) => throw new AssertionError("Already published!")
        case Halted | Killed => {}
      }
    }

    def halt() {
      state match {
        case Published(_) | Live | Blocked(_) | Suspending(_) => {
          state match {
            case Blocked(handle: SiteCallHandle) => handle.kill()
            case _ => {}
          }

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
  def leftToRight[X, Y](f: X => (Y => Unit) => Unit, xs: List[X])(k: List[Y] => Unit): Unit = {
    def walk(xs: List[X], ys: List[Y]): Unit = {
      xs match {
        case z :: zs => f(z) { y: Y => walk(zs, y :: ys) }
        case Nil => k(ys.reverse)
      }
    }
    walk(xs, Nil)
  }

}