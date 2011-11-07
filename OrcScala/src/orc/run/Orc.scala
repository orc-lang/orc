//
// Orc.scala -- Scala class Orc
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import orc.{ OrcExecutionOptions, CaughtEvent, HaltedEvent, PublishedEvent, OrcEvent, Handle, OrcRuntime }
import orc.values.OrcRecord
import orc.values.Field
import orc.ast.oil.nameless._
import orc.error.OrcException
import orc.error.runtime.{ ArityMismatchException, TokenException, StackLimitReachedError, TokenError, TokenLimitReachedError }
import scala.collection.mutable.Set

trait Orc extends OrcRuntime {

  def run(node: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    startScheduler(options: OrcExecutionOptions)
    val root = new Orc.this.Execution(node, k, options)
    val t = new Token(node, root)
    schedule(t)
  }

  def stop() = {
    stopScheduler()
  }

  ////////
  // Groups
  ////////

  // A Group is a structure associated with dynamic instances of an expression,
  // tracking all of the executions occurring within that expression.
  // Different combinators make use of different Group subclasses.

  sealed trait GroupMember {
    def kill(): Unit
    def suspend(): Unit
    def resume(): Unit
    def notifyOrc(event: OrcEvent): Unit
  }

  sealed trait Blocker

  trait Group extends GroupMember with Runnable {
    def publish(t: Token, v: AnyRef): Unit
    def onHalt(): Unit

    var members: Set[GroupMember] = Set()

    var pendingKills: Set[GroupMember] = Set()

    def halt(t: Token) = synchronized { remove(t) }

    /* Note: this is _not_ lazy termination */
    def kill() = synchronized {
      pendingKills ++= members
      schedule(this)
    }

    def run() {
      try {
        pendingKills.map(_.kill)
      } catch {
        case e: InterruptedException => throw e
        case e => { notifyOrc(CaughtEvent(e)) }
      }
    }

    def suspend() = synchronized {
      for (m <- members) m.suspend()
    }

    def resume() = synchronized {
      for (m <- members) m.resume()
    }

    def add(m: GroupMember) {
      synchronized {
        members.add(m)
      }
      m match {
        case t: Token if (root.options.maxTokens > 0) => {
          if (root.tokenCount.incrementAndGet() > root.options.maxTokens)
            throw new TokenLimitReachedError(root.options.maxTokens)
        }
        case _ => {}
      }
    }

    def remove(m: GroupMember) {
      synchronized {
        members.remove(m)
        if (members.isEmpty) { onHalt }
      }
      m match {
        case t: Token if (root.options.maxTokens > 0) => root.tokenCount.decrementAndGet()
        case _ => {}
      }
    }

    def inhabitants: List[Token] =
      members.toList flatMap {
        case t: Token => List(t)
        case g: Group => g.inhabitants
      }

    /* Find the root of this group tree. */
    val root: Execution

  }

  abstract class Subgroup(parent: Group) extends Group {

    override def kill() = synchronized { super.kill(); parent.remove(this) }
    def notifyOrc(event: OrcEvent) = parent.notifyOrc(event)

    override val root = parent.root

    parent.add(this)

  }

  /** Possible states of a Groupcell */
  class GroupcellState
  case class Unbound(waitlist: List[Token]) extends GroupcellState
  case class Bound(v: AnyRef) extends GroupcellState
  case object Dead extends GroupcellState

  /** A Groupcell is the group associated with expression g in (f <x< g) */
  class Groupcell(parent: Group) extends Subgroup(parent) with Blocker {

    var state: GroupcellState = Unbound(Nil)

    def publish(t: Token, v: AnyRef) = synchronized {
      state match {
        case Unbound(waitlist) => {
          state = Bound(v)
          t.halt()
          this.kill()
          for (t <- waitlist) { t.publish(Some(v)) }
        }
        case _ => t.halt()
      }
    }

    def onHalt() = synchronized {
      state match {
        case Unbound(waitlist) => {
          state = Dead
          parent.remove(this)
          for (t <- waitlist) { t.publish(None) }
        }
        case _ => {}
      }
    }

    // Specific to Groupcells
    def read(t: Token) = synchronized {
      state match {
        case Bound(v) => t.publish(Some(v))
        case Dead => t.publish(None)
        case Unbound(waitlist) => {
          t.blockOn(this)
          state = Unbound(t :: waitlist)
        }
      }
    }

  }

  /** A Region is the group associated with expression f in (f ; g) */
  class Region(parent: Group, t: Token) extends Subgroup(parent) with Blocker {

    /* Some(t): No publications have left this region.
     *          If the group halts silently, t will be scheduled.
     *
     *    None: One or more publications has left this region.
     */
    t.blockOn(this)

    var pending: Option[Token] = Some(t)

    def publish(t: Token, v: AnyRef) = synchronized {
      pending foreach { _.halt() } // Remove t from its group
      pending = None
      t.migrate(parent).publish(v)
    }

    def onHalt() = synchronized {
      pending foreach { _.unblock }
      parent.remove(this)
    }

  }

  /** A type alias for Orc event handlers */
  type OrcHandler = PartialFunction[OrcEvent, Unit]

  /**
   * Generate the list of event handlers for an execution
   * Traits which add support for more events will override this
   * method and introduce more handlers.
   */
  def generateOrcHandlers(host: Execution): List[OrcHandler] = Nil

  /**
   * An execution is a special toplevel group,
   * associated with the entire program.
   */
  class Execution(private[run] var _node: Expression, k: OrcEvent => Unit, private[run] var _options: OrcExecutionOptions) extends Group {

    def node = _node;
    def options = _options;

    val tokenCount = new java.util.concurrent.atomic.AtomicInteger(0);

    def publish(t: Token, v: AnyRef) = synchronized {
      k(PublishedEvent(v))
      t.halt()
    }

    def onHalt() = synchronized {
      k(HaltedEvent)
    }

    val eventHandler: OrcEvent => Unit = {
      val handlers = generateOrcHandlers(this)
      val baseHandler = {
        case e@ CaughtEvent(_: Error) => { Orc.this.stop; k(e) }
        case e@ CaughtEvent(_: TokenError) => { kill(); k(e) }
        case e => k(e)
      }: PartialFunction[OrcEvent, Unit]
      def composeOrcHandlers(f: OrcHandler, g: OrcHandler) = f orElse g
      handlers.foldRight(baseHandler)(composeOrcHandlers)
    }

    def notifyOrc(event: OrcEvent) = { eventHandler(event) }

    override val root = this

  }

  ////////
  // Bindings
  ////////

  trait Binding
  case class BoundValue(v: AnyRef) extends Binding
  case object BoundStop extends Binding
  case class BoundCell(g: Groupcell) extends Binding

  case class Closure(private[run] var _defs: List[Def], pos: Int, lexicalContext: List[Binding]) {

    def defs = _defs

    def code: Def = defs(pos)

    def context: List[Binding] = {
      val fs =
        for (i <- defs.indices) yield {
          BoundValue(Closure(defs, i, lexicalContext))
        }
      fs.toList.reverse ::: lexicalContext
    }

    //    override def toString() = {
    //      val (defs, rest) = context.splitAt(ds.size)
    //      val newctx = (defs map {_ => None}) ::: (rest map { Some(_) })
    //      val subdef = defs(pos).subst(context map { Some(_) })
    //      }
    //    val myName = new BoundVar()
    //    val defNames =
    //      for (d <- defs) yield
    //        if (d == this) { myName } else { new BoundVar() }
    //    val namedDef = namelessToNamed(myName, subdef, defNames, Nil)
    //    val pp = new PrettyPrint()
    //    "lambda" +
    //      pp.reduce(namedDef.name) +
    //        pp.paren(namedDef.formals) +
    //          " = " +
    //            pp.reduce(namedDef.body)
    //    }

  }

  ////////
  // Frames
  ////////

  abstract class Frame {
    def apply(t: Token, v: AnyRef): Unit
  }

  case class BindingFrame(n: Int) extends Frame {
    def apply(t: Token, v: AnyRef) {
      t.env = t.env.drop(n)
      t.publish(v)
    }
  }

  case class SequenceFrame(private[run] var _node: Expression) extends Frame {
    def node = _node
    def apply(t: Token, v: AnyRef) {
      schedule(t.bind(BoundValue(v)).move(node))
    }
  }

  case class FunctionFrame(private[run] var _callpoint: Expression, env: List[Binding]) extends Frame {
    def callpoint = _callpoint
    def apply(t: Token, v: AnyRef) {
      t.functionFramesPushed = t.functionFramesPushed - 1
      t.env = env
      t.move(callpoint).publish(v)
    }
  }

  case class KFrame(private[run]_k: (Option[AnyRef] => Unit)) extends Frame {
    def k = _k
    def apply(t: Token, v: AnyRef) {
      val _v = v.asInstanceOf[Option[AnyRef]]
      k(_v)
    }
  }

  case object GroupFrame extends Frame {
    def apply(t: Token, v: AnyRef) {
      t.group.publish(t, v)
    }
  }

  ////////
  // Tokens
  ////////

  sealed trait TokenState
  /** Token is ready to make progress */
  case object Live extends TokenState
  /** Token is waiting on another task */
  case class Blocked(blocker: Blocker) extends TokenState
  /** Token has been told to suspend, but it's still in the scheduler queue */
  case class Suspending(prevState: TokenState) extends TokenState
  /** Suspended Tokens must be re-scheduled upon resume */
  case class Suspended(prevState: TokenState) extends TokenState
  /** Token halted itself */
  case object Halted extends TokenState
  /** Token killed by engine */
  case object Killed extends TokenState
  /** Live Token with a value to propagate */
  case class Published(v: AnyRef) extends TokenState

  class SiteCallHandle(caller: Token, calledSite: AnyRef, actuals: List[AnyRef]) extends Handle with Blocker {

    var listener: Option[Token] = Some(caller)
    var invocationThread: Option[Thread] = None

    caller.blockOn(this)

    def run() {
      try {
        if (synchronized {
          if (listener.isDefined) {
            invocationThread = Some(Thread.currentThread)
          }
          listener.isDefined
        }) {
          invoke(this, calledSite, actuals)
        }
      } catch {
        case e: OrcException => this !! e
        case e: InterruptedException => { halt(); throw e }  //Thread interrupt causes halt without notify
        case e => { notifyOrc(CaughtEvent(e)); halt() }
      } finally {
        synchronized {
          invocationThread = None
        }
      }
    }

    def publish(v: AnyRef) =
      synchronized {
        listener foreach { _ publish v }
      }

    def halt() =
      synchronized {
        listener foreach { _.halt() }
      }

    def !!(e: OrcException) =
      synchronized {
        listener foreach { _ !! e }
      }

    def notifyOrc(event: orc.OrcEvent) =
      synchronized {
        listener foreach { _ notifyOrc event }
      }

    def isLive =
      synchronized {
        listener.isDefined
      }

    def kill() =
      synchronized {
        /* Called from the listener's Token.kill, so the listener is not notified. */
        listener = None
        invocationThread foreach { _.interrupt() }
      }

  }

  class Token private (
    var node: Expression,
    var stack: List[Frame] = Nil,
    var env: List[Binding] = Nil,
    var group: Group,
    var state: TokenState = Live) extends GroupMember with Runnable {

    var functionFramesPushed: Int = 0;

    /** Public constructor */
    def this(start: Expression, g: Group) = {
      this(node = start, group = g, stack = List(GroupFrame))
    }

    def runtime = Orc.this

    def options = group.root.options

    /** Copy constructor with defaults */
    private def copy(
      node: Expression = node,
      stack: List[Frame] = stack,
      env: List[Binding] = env,
      group: Group = group,
      state: TokenState = state): Token = {

      new Token(node, stack, env, group, state)
    }

    // A live token is added to its group when it is created
    state match {
      case Published(_) | Live | Blocked(_) | Suspending(_) | Suspended(_) => group.add(this)
      case Halted | Killed => {}
    }

    def notifyOrc(event: OrcEvent) { group.notifyOrc(event) }

    def kill() = synchronized {
      state match {
        case Published(_) | Live | Blocked(_) | Suspending(_) | Suspended(_) => {
          state match {
            case Blocked(handle: SiteCallHandle) => handle.kill()
            case _ => {}
          }

          state = Killed
          group.halt(this)
        }
        case Halted | Killed => {}
      }
    }

    def blockOn(blocker: Blocker) = synchronized {
      state match {
        case Live => state = Blocked(blocker)
        case Killed => {}
        case _ => throw new AssertionError("Only live tokens may be blocked: state=" + state)
      }
    }

    def unblock() = synchronized {
      state match {
        case Blocked(_: Region) => {
          state = Live
          schedule(this)
        }
        case Killed => {}
        case Suspending(Blocked(_: Region)) => {
          state = Suspending(Live)
          schedule(this)
        }
        case Suspended(Blocked(_: Region)) => {
          state = Suspended(Live)
        }
        case Blocked(_) => { throw new AssertionError("Tokens may only receive _.unblock from a region") }
        case _ => { throw new AssertionError("unblock on a Token that is not Blocked/Killed: state=" + state) }
      }
    }

    def suspend() = synchronized {
      state match {
        case Live | Blocked(_) | Published(_) => state = Suspending(state)
        case Suspending(_) | Suspended(_) | Halted | Killed => {}
      }
    }

    def resume() = synchronized {
      state match {
        case Suspending(prevState) => state = prevState
        case Suspended(prevState) => {
          state = prevState
          schedule(this)
        }
        case Published(_) | Live | Blocked(_) | Halted | Killed => {}
      }
    }

    def fork() = { (this, copy()) }

    def move(e: Expression) = { node = e; this }

    def push(f: Frame) = { stack = f :: stack; this }

    def migrate(newGroup: Group) = {
      val oldGroup = group
      newGroup.add(this); oldGroup.remove(this)
      group = newGroup
      this
    }

    def join(newGroup: Group) = { this.push(GroupFrame).migrate(newGroup) }

    def bind(b: Binding): Token = {
      env = b :: env
      stack match {
        case BindingFrame(n) :: fs => { stack = (new BindingFrame(n + 1)) :: fs }

        /* Tail call optimization (part 1 of 2) */
        case FunctionFrame(_, _) :: fs if (!options.disableTailCallOpt) => { /* Do not push a binding frame over a tail call.*/ }

        case fs => { stack = BindingFrame(1) :: fs }
      }
      this
    }

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
        case BoundCell(g) => stack = KFrame(k) :: stack; g read this // TODO: push k on stack before calling read
      }
    }

    /**
     * Create a new Closure object whose lexical bindings are all resolved and replaced.
     * Such a closure will have no references to any Groupcell.
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
              this !! new StackLimitReachedError(options.stackSize)
            push(new FunctionFrame(node, env))
          }
        }

        /* Jump into the function context */
        this.env = context

        /* Bind the args */
        for (p <- params) { bind(p) }

        /* Jump into the function body */
        schedule(this.move(d.body))
      }
    }

    def siteCall(s: AnyRef, actuals: List[AnyRef]): Unit = {
      val sh = new SiteCallHandle(this, s, actuals)
      state = Blocked(sh)
      schedule(sh)
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

    def isLive = { state = Live }

    def run() {
      try {
        var runNode = false
        synchronized {
          state match {
            case Live => runNode = true // Run this token's current AST node, ouside this synchronized block
            case Blocked(_) => throw new AssertionError("blocked token scheduled")
            case Suspending(prevState) => state = Suspended(prevState)
            case Suspended(_) => throw new AssertionError("suspended token scheduled")
            case Halted => throw new AssertionError("halted token scheduled")
            case Killed => {} // This token was killed while it was on the schedule queue; ignore it
            case Published(v) => {
              state = Live
              stack match {
                case f :: fs => {
                  stack = fs
                  f(this, v)
                }
                case List() => {
                  throw new AssertionError("publish on an empty stack")
                }
              }
            }
          }
        }
        if (runNode) eval(node)
      } catch {
        case e: OrcException => this !! e
        case e: InterruptedException => { halt(); throw e }  //Thread interrupt causes halt without notify
        case e => { notifyOrc(CaughtEvent(e)); halt() }
      }
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
          val (l, r) = fork
          schedule(l.move(left), r.move(right))
        }

        case Sequence(left, right) => {
          val frame = new SequenceFrame(right)
          schedule(this.push(frame).move(left))
        }

        case Prune(left, right) => {
          val (l, r) = fork
          val groupcell = new Groupcell(group)
          schedule(l.bind(BoundCell(groupcell)).move(left),
            r.join(groupcell).move(right))
        }

        case Otherwise(left, right) => {
          val (l, r) = fork
          val region = new Region(group, r.move(right))
          schedule(l.join(region).move(left))
        }

        case decldefs @ DeclareDefs(openvars, defs, body) => {
          /* Closure compaction: Bind only the free variables
           * of the defs in this lexical context.
           */
          val lexicalContext = openvars map { i: Int => lookup(Variable(i)) }
          for (i <- defs.indices) {
            bind(BoundValue(Closure(defs, i, lexicalContext)))
          }
          schedule(this.move(body))
        }

        case HasType(expr, _) => this.move(expr).run

        case DeclareType(_, expr) => this.move(expr).run
      }
    }

    // Publicly accessible methods

    def publish(v: AnyRef) {
      state match {
        case Blocked(_: Region) => throw new AssertionError("publish on a pending Token")
        case Live | Blocked(_) => {
          state = Published(v)
          schedule(this)
        }
        case Suspending(_) => {
          state = Suspending(Published(v))
          schedule(this)
        }
        case Suspended(_) => {
          state = Suspended(Published(v))
        }
        case Published(_) => throw new AssertionError("Already published!")
        case Halted | Killed => {}
      }
    }

    def halt() {
      state match {
        case Published(_) | Live | Blocked(_) | Suspending(_) => {
          state = Halted
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
