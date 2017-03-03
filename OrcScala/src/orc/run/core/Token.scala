//
// Token.scala -- Scala class Token
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import orc.{ CaughtEvent, OrcEvent, OrcRuntime, Schedulable }
import orc.ast.oil.nameless._
import orc.ast.oil.nameless.Site
import orc.error.OrcException
import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException, DoesNotHaveMembersException, StackLimitReachedError, TokenException }
import orc.lib.time.{ Vawait, Vtime }
import orc.run.Logger
import orc.run.distrib.{ DOrcExecution, NoLocationAvailable, PeerLocation }
import orc.values.{ Field, HasMembers, OrcObject, OrcRecord, Signal }
import orc.values.sites.TotalSite

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
  protected var stack: Frame,
  protected var env: List[Binding],
  protected var group: Group,
  protected var clock: Option[VirtualClock],
  protected var state: TokenState,
  val debugId: Long)
  extends GroupMember with Schedulable with Blockable with Resolver {

  /** Convenience constructor with defaults  */
  protected def this(
    node: Expression,
    stack: Frame = EmptyFrame,
    env: List[Binding] = Nil,
    group: Group,
    clock: Option[VirtualClock] = None,
    state: TokenState = Live) = {
    this(node, stack, env, group, clock, state, Token.getNextTokenDebugId(group.runtime))
  }

  var functionFramesPushed: Int = 0

  // These fields may be useful for debugging multiple scheduling or multiple run bugs.
  // Uses of them are marked with "MULTI_SCHED_DEBUG"
  /*
  val isScheduled = new AtomicBoolean(false)
  val isRunning = new AtomicBoolean(false)
  val schedulingThread = new AtomicReference[Thread](null)
  */

  def runtime: OrcRuntime = group.runtime
  def execution = group.execution

  def sourcePosition = node.sourceTextRange

  def options = group.options

  /** Execution of a token cannot indefinitely block the executing thread. */
  override val nonblocking = true

  /** Public constructor */
  def this(start: Expression, g: Group) = {
    this(node = start, group = g, stack = GroupFrame(EmptyFrame))
    Tracer.traceTokenCreation(this, this.state)
  }

  /** Copy constructor with defaults */
  private def copy(
    node: Expression = node,
    stack: Frame = stack,
    env: List[Binding] = env,
    group: Group = group,
    clock: Option[VirtualClock] = clock,
    state: TokenState = state): Token = {
    //Logger.check(stack.forall(!_.isInstanceOf[FutureFrame]), "Stack being used in copy contains FutureFrames: " + stack)
    val newToken = new Token(node, stack, env, group, clock, state)
    Tracer.traceTokenCreation(newToken, state)
    newToken
  }

  /*
   * On creation: Add a token to its group if it is not halted or killed.
   *
   * All initialization that must occur before run() executes must happen
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
      getClass.getName + (if (recursing eq null) f"(debugId=$debugId%#x,state=$state, stackTop=$stack, node=$node, node.sourceTextRange=${node.sourceTextRange}, group=$group, clock=$clock)" else "")
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
    if (state != Killed) {
      // Logger.finer(s"Changing state: $this to $newState")
      Tracer.traceTokenStateTransition(this, state, newState)
      state = newState
      true
    } else {
      false
    }
  }

  /** An expensive walk-to-root check for alive state */
  def checkAlive(): Boolean = state.isLive && group.checkAlive()

  override def setQuiescent() { clock foreach { _.setQuiescent() } }

  override def unsetQuiescent() { clock foreach { _.unsetQuiescent() } }

  /* When a token is scheduled, notify its clock accordingly */
  override def onSchedule() {
    Tracer.traceTokenExecStateTransition(this, TokenExecState.Scheduled)

    // MULTI_SCHED_DEBUG
    /*
    val old = isScheduled.getAndSet(true)
    if (!(old == false || group.isKilled())) {
      Logger.check(false, s"""${System.nanoTime().toHexString}: Failed to set scheduled: ${this.debugId.toHexString}""")
      orc.util.Tracer.dumpOnlyLocation(debugId)
    }

    val curr = Thread.currentThread()
    val prev = schedulingThread.getAndSet(curr)
    if(!(curr == prev || prev == null)) {
      val trace = StackTrace.getStackTrace(3, 1)
      println(s"${System.nanoTime().toHexString}: Scheduling from a new thread: Was ${prev.getId.toHexString}, now ${curr.getId.toHexString}. ${this.debugId.toHexString} ${trace.mkString("; ")}")
    }*/

    unsetQuiescent()
  }

  /* When a token is finished running, notify its clock accordingly */
  override def onComplete() {
    setQuiescent()
    // MULTI_SCHED_DEBUG
    //Logger.check(isRunning.compareAndSet(true, false) || state == Killed || Token.isRunningAlreadyCleared.get, s"""${System.nanoTime().toHexString}: Failed to clear running: $this""")
    //Token.isRunningAlreadyCleared.set(false)
    Tracer.traceTokenExecStateTransition(this, TokenExecState.DoneRunning)
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
        case Blocked(handle: ExternalSiteCallHandle) => Some(handle)
        case Live | Publishing(_) | Blocked(_) | Halted | Killed => None
      }
    }
    Tracer.traceTokenExecStateTransition(this, TokenExecState.Killed)
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
      case _ => throw new AssertionError("Only live tokens may be blocked: state=" + state + "   " + this)
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
      case Blocked(_: OtherwiseGroup) => {
        if (setState(Live)) { runtime.stage(this) }
      }
      case Suspending(Blocked(_: OtherwiseGroup)) => {
        if (setState(Suspending(Live))) { runtime.stage(this) }
      }
      case Suspended(Blocked(_: OtherwiseGroup)) => {
        setState(Suspended(Live))
      }
      case Killed => {}
      case _ => { throw new AssertionError("unblock on a Token that is not Blocked(OtherwiseGroup)/Killed: state=" + state) }
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
  //def getState(): TokenState = { state }

  def migrate(newGroup: Group) = {
    require(newGroup != group)
    val oldGroup = group
    newGroup.add(this)
    val removeSucceeded = oldGroup.remove(this)
    // If the remove failed we kill instead of switching groups.
    // We also remove ourselves from the new group.
    if(removeSucceeded) {
      group = newGroup
    } else {
      newGroup.remove(this)
      kill()
    }
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
      case vr @ Variable(n) => {
        val v = env(n)
        v
      }
      case UnboundVariable(x) =>
        Logger.severe(s"Stopping token due to unbound variable $a at ${a.sourceTextRange}")
        BoundStop
    }
  }

  protected def functionCall(d: Def, context: List[Binding], params: List[Binding]) {
    Logger.fine(s"Calling $d with $params")
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

  protected def orcSiteCall(s: OrcSite, params: List[AnyRef]) {
    if (params.size != s.code.arity) {
      this !! new ArityMismatchException(s.code.arity, params.size) /* Arity mismatch. */
    } else {
      val sh = new OrcSiteCallHandle(this)
      blockOn(sh)

      // TODO: Implement TCO for OrcSite calls. By reusing the OrcSiteCallHandle? When is it safe?

      // Just build the stack instead of pushing after we create it.
      // The parameters go on in reverse order. First parameter on the "bottom" of the arguments.
      val env = (params map BoundValue).reverse ::: s.context

      // Build a token that is in a group nested inside the declaration context.
      val t = new Token(s.code.body,
        env = env,
        group = new OrcSiteCallGroup(s.group, sh),
        stack = GroupFrame(EmptyFrame),
        clock = s.clock)

      runtime.stage(t)
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
    Logger.fine(s"Calling $s with $actuals")
    assert(!s.isInstanceOf[Binding])
    //FIXME:Refactor: Place in correct classes, not all here
    /* Maybe there's an extension mechanism we need to add to Orc here.
     * 'Twould be nice to also move the Vclock hook below to this mechanism. */
    def pickLocation(ls: Set[PeerLocation]) = ls.head

    group.execution match {
      case dOrcExecution: DOrcExecution => {
        orc.run.distrib.Logger.entering(getClass.getName, "siteCall", Seq(s.getClass.getName, s, actuals))
        val intersectLocs = (actuals map dOrcExecution.currentLocations).fold(dOrcExecution.currentLocations(s)) { _ & _ }
        if (!(intersectLocs contains dOrcExecution.runtime.here)) {
          orc.run.distrib.Logger.finest(s"siteCall($s,$actuals): intersection of current locations=$intersectLocs")
          val candidateDestinations = {
            if (intersectLocs.nonEmpty) {
              intersectLocs
            } else {
              val intersectPermittedLocs = (actuals map dOrcExecution.permittedLocations).fold(dOrcExecution.permittedLocations(s)) { _ & _ }
              if (intersectPermittedLocs.nonEmpty) {
                intersectPermittedLocs
              } else {
                throw new NoLocationAvailable(s +: actuals)
              }
            }
          }
          orc.run.distrib.Logger.finest(s"candidateDestinations=$candidateDestinations")
          val destination = pickLocation(candidateDestinations)
          dOrcExecution.sendToken(this, destination)
          return
        }
      }
      case _ => /* Not a distributed execution */
    }
    //End of code needing refactoring
    s match {
      case vc: VirtualClockOperation => {
        clockCall(vc, actuals)
      }
      case s: OrcSite => {
        orcSiteCall(s, actuals)
      }
      case _ => {
        val sh = new ExternalSiteCallHandle(this, s, actuals)
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
      // Check for HasMembers, but only on non-sites and OrcRecords
      // TODO: This is a horrible list of special cases. We should remove this need by fully disentangling sites and values.
      case o: HasMembers if (!o.isInstanceOf[orc.values.sites.Site] || o.isInstanceOf[orc.values.OrcRecord]) && o.hasMember(Field("apply")) => {
        resolve(o.getMember(Field("apply"))) { makeCall(_, params) }
      }
      case s => {
        params match {
          /* Zero parameters. No need to block. */
          case Nil => {
            siteCall(s, Nil)
          }

          /* One parameter. May need to block. No need to join. */
          case List(param) => {
            resolve(param) { arg: AnyRef =>
              if (arg.isInstanceOf[Field]) {
                Logger.warning(s"Field call to site is no longer supported: $s($arg)")
              }
              siteCall(s, List(arg))
            }
          }

          /* Multiple parameters. May need to join. */
          case _ => {
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
          val i = orderingSite.evaluate(Array(x, y)).asInstanceOf[Int]
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

  def newObject(bindings: Map[Field, Expression]) {
    val self = new OrcObject()

    Logger.fine(s"Instantiating class: ${bindings}")
    val objenv = BoundValue(self) :: env

    val fields = for ((name, expr) <- bindings) yield {
      expr match {
        // NOTE: The first two cases are optimizations to avoid creating a group and a token for simple fields.
        case Constant(v) => {
          (name, BoundValue(v))
        }
        case Variable(n) => {
          (name, objenv(n))
        }

        case _ => {
          // We use a GraftGroup since it is exactly what we need.
          // The difference between this and graft is where the future goes.
          val pg = new GraftGroup(group)

          // A binding frame is not needed since publishing will trigger the token to halt.
          val t = new Token(expr,
            env = objenv,
            group = pg,
            stack = GroupFrame(EmptyFrame),
            clock = clock)
          runtime.stage(t)

          (name, pg.binding)
        }
      }
    }
    Logger.fine(s"Setup binding for fields: $fields")
    self.setFields(fields.toMap)

    publish(Some(self))
  }

  //def stackOK(testStack: Array[java.lang.StackTraceElement], offset: Int): Boolean =
  //  testStack.length == 4 + offset && testStack(1 + offset).getMethodName() == "runTask" ||
  //    testStack(1 + offset).getMethodName() == "eval" && testStack(2 + offset).getMethodName() == "run" && stackOK(testStack, offset + 2)

  def run() {
    Tracer.traceTokenExecStateTransition(this, TokenExecState.Running)
    //val ourStack = new Throwable("Entering Token.run").getStackTrace()
    //assert(stackOK(ourStack, 0), "Token run not in ThreadPoolExecutor.Worker! sl="+ourStack.length+", m1="+ourStack(1).getMethodName()+", state="+state)
    // MULTI_SCHED_DEBUG
    // Add this yeild to increase the odds of thread interleaving. Such as a kill happening while the token is running.
    //Thread.`yield`()
    // MULTI_SCHED_DEBUG
    /*
    val old = isScheduled.getAndSet(false)
    if (!(old == true || state == Killed)) {
      Logger.check(false, s"""${System.nanoTime().toHexString}: Failed to clear scheduled: ${this.debugId.toHexString}""")
      orc.util.Tracer.dumpOnlyLocation(debugId)
    }
    */
    try {
      if (group.isKilled()) { kill() }
      // MULTI_SCHED_DEBUG
      //Logger.check(isRunning.compareAndSet(false, true) || state == Killed, s"${System.nanoTime().toHexString}: Failed to set running: $this")
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

  override def resolveOptional(b: Binding)(k: Option[AnyRef] => Unit) = {
    // MULTI_SCHED_DEBUG
    //Token.isRunningAlreadyCleared.set(true)
    //Logger.check(isRunning.compareAndSet(true, false) || state == Killed || Token.isRunningAlreadyCleared.get, s"${System.nanoTime().toHexString}: Failed to clear running: $this")
    super.resolveOptional(b)(k)
  }

  protected def eval(node: orc.ast.oil.nameless.Expression) {
    //Logger.finest(s"Evaluating: $node")
    node match {
      case Stop() => halt()

      case Hole(_, _) => halt()

      case (a: Argument) => {
        resolve(lookup(a)) { v => publish(Some(v)) }
      }

      case Call(target, args, _) => {
        val params = args map lookup
        lookup(target) match {
          /*
           * Allow a def to be called with an open context.
           * This functionality is sound, but technically exceeds the formal semantics of Orc.
           */
          case BoundReadable(c: Closure) => functionCall(c.code, c.context, params)

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

      case Graft(value, body) => {
        val (v, b) = fork()
        val pg = new GraftGroup(group)
        b.bind(pg.binding)
        v.join(pg)
        v.move(value)
        b.move(body)
        runtime.stage(v, b)
      }

      case Trim(expr) => {
        val g = new TrimGroup(group)
        join(g)
        move(expr)
        runtime.stage(this)
      }

      case Otherwise(left, right) => {
        val (l, r) = fork
        r.move(right)
        val region = new OtherwiseGroup(group, r)
        l.join(region)
        l.move(left)
        runtime.stage(l)
      }

      case New(_, bindings, _) => {
        newObject(bindings)
      }

      case FieldAccess(o, f) => {
        resolve(lookup(o)) {
          _ match {
            case o: HasMembers =>
              //Logger.finer(s"resolving $o$f")
              resolve(o.getMember(f)) { x =>
                //Logger.finer(s"resolved $o$f = $x")
                publish(Some(x))
              }
            // Fallback on old call style fields
            // TODO: Remove the need for this.
            case s: AnyRef =>
              siteCall(s, List(f))
            case null =>
              throw new DoesNotHaveMembersException(null)
          }
        }
      }

      case VtimeZone(timeOrdering, body) => {
        resolve(lookup(timeOrdering)) { newVclock(_, body) }
      }

      case decldefs @ DeclareCallables(openvars, decls, body) => {
        /* Closure compaction: Bind only the free variables
         * of the defs in this lexical context.
         */
        val lexicalContext = openvars map { i: Int => env(i) }

        decls.head match {
          case _: Def => {
            val closureGroup = new ClosureGroup(decls.collect({ case d: Def => d }), lexicalContext, runtime)
            runtime.stage(closureGroup)

            for (c <- closureGroup.members) {
              bind(BoundReadable(c))
            }
          }
          case _: Site => {
            val sites = for (s <- decls) yield new OrcSite(s.asInstanceOf[Site], group, clock)

            val context = (sites map BoundValue) ::: lexicalContext

            for (s <- sites) {
              s.context = context
              bind(BoundValue(s))
            }
          }
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
    v foreach { vv =>
      assert(!vv.isInstanceOf[Binding], s"Interpreter bug. Triggered at $this")
      assert(!vv.isInstanceOf[java.math.BigInteger], s"Type coercion error at $this")
      assert(!vv.isInstanceOf[java.math.BigDecimal], s"Type coercion error at $this")
    }
    state match {
      case Blocked(_: OtherwiseGroup) => throw new AssertionError("publish on a pending Token")
      case Live => {
        // If we are live then publish normally.
        setState(Publishing(v))
        runtime.stage(this)
      }
      case Blocked(_) => {
        if (v.isDefined) {
          // If we are blocking then publish in a copy of this token.
          // This is needed to allow blockers to publish more than once.
          val nt = copy(state = Publishing(v))
          runtime.stage(nt)
        } else {
          // However "publishing" stop is handled without duplication.
          setState(Publishing(v))
          runtime.stage(this)
        }
      }
      case Suspending(_) => {
        throw new AssertionError("Suspension is not supported anymore.")
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

  def discorporate() {
    state match {
      case Publishing(_) | Live | Blocked(_) | Suspending(_) => {
        group.discorporate(this)
      }
      case Suspended(_) => throw new AssertionError("discorporate on a suspended Token")
      case Halted | Killed => {}
    }
  }

  def !!(e: OrcException) {
    e.setPosition(node.sourceTextRange.orNull)
    e match {
      case te: TokenException if (te.getBacktrace() == null || te.getBacktrace().length == 0) => {
        val callPoints = stack.toList collect { case f: FunctionFrame => f.callpoint.sourceTextRange.orNull }
        te.setBacktrace(callPoints.toArray)
      }
      case _ => {} // Not a TokenException; no need to collect backtrace
    }
    notifyOrc(CaughtEvent(e))
    halt()
  }

  override def awakeTerminalValue(v: AnyRef) = {
    setState(Live)
    publish(Some(v))
  }
  def awakeNonterminalValue(v: AnyRef) = {
    publish(Some(v))
  }
  def awakeStop() = publish(None)

  override def awakeException(e: OrcException) = this !! e

  override def awake() { unblock() }

  // DEBUG CODE:
  def envToString() = {
    env.zipWithIndex.map({
      case (b, i) => s"$i: " + (b match {
        case BoundValue(v) => v.toString
        case BoundReadable(c: Closure) => c.code.toString
        case BoundReadable(c) => c.toString
        case BoundStop => "stop"
      })
    }).mkString("\n")
  }
}

private class LongCounter(private var value: Long) {
  def incrementAndGet() = {
    value += 1L
    value
  }
}

object Token {
  // MULTI_SCHED_DEBUG
  /*
  private val isRunningAlreadyCleared = new ThreadLocal[Boolean]() {
    override def initialValue() = false
  }
  */

  private val currentTokenDebugId = new ThreadLocal[LongCounter]() {
    override def initialValue() = new LongCounter(0L)
  }
  def getNextTokenDebugId(runtime: OrcRuntime): Long =
    /* FIXME:This adverse coupling to runtime should be removed. Why not add a runtimeThreadId to the Orc trait? */
    currentTokenDebugId.get.incrementAndGet() | (runtime.asInstanceOf[orc.run.Orc].runtimeDebugThreadId.toLong << 32)
}

/** Supertype of TokenStates */
sealed abstract class TokenState {
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
  override def toString() = s"$productPrefix($blocker : ${blocker.getClass.getSimpleName})"
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

/** Supertype of TokenExecStates.
  *
  * These are not actually used or stored other than for tracing.
  */
sealed abstract class TokenExecState

object TokenExecState {
  /** Token is executing on some thread */
  case object Running extends TokenExecState

  /** Token is scheduled to execute */
  case object Scheduled extends TokenExecState

  /** Token is waiting or blocked on some event.
    *
    * That event will trigger the token to be scheduled.
    */
  case object DoneRunning extends TokenExecState

  /** Token has been killed */
  case object Killed extends TokenExecState
}
