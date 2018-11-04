//
// Counter.scala -- Scala class Counter
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, LongAdder }

import orc.run.extensions.SimpleWorkStealingScheduler
import orc.run.porce.{ Logger, SimpleWorkStealingSchedulerWrapper }
import orc.util.{ DumperRegistry, SummingStopWatch, Tracer }

import com.oracle.truffle.api.CompilerDirectives.{ CompilationFinal, TruffleBoundary }
import com.oracle.truffle.api.CompilerDirectives
import java.util.logging.Level
import com.oracle.truffle.api.profiles.ConditionProfile

@CompilationFinal
object Counter {
  import CounterConstants._

  val CounterNestedCreated = 101L
  Tracer.registerEventTypeId(CounterNestedCreated, "CtrNestC", _.formatted("%016x"), _.formatted("%016x"))
  val CounterTerminatorCreated = 102L
  Tracer.registerEventTypeId(CounterTerminatorCreated, "CtrTermC", _.formatted("%016x"), _.formatted("%016x"))
  val CounterServiceCreated = 103L
  Tracer.registerEventTypeId(CounterServiceCreated, "CtrServC", _.formatted("%016x"), _.formatted("%016x"))

  @inline @CompilationFinal
  private val atomicOperationTimer = SummingStopWatch.maybe(enableTiming)

  def startTimer(): Long = if (enableTiming) startTimerReal else 0
  def stopTimer(s: Long) = if (enableTiming) stopTimerReal(s)

  @TruffleBoundary(allowInlining = true) @noinline
  def startTimerReal(): Long = if (SummingStopWatch.enabled) atomicOperationTimer.start() else 0
  @TruffleBoundary(allowInlining = true) @noinline
  def stopTimerReal(s: Long) = if (SummingStopWatch.enabled) atomicOperationTimer.stop(s)

  if (SummingStopWatch.enabled && enableTiming) {
    DumperRegistry.registerCSVLineDumper("counter-timers", "csv", "counter times",
        Seq(
            "Dump ID [id]",
            "Counter Time [time]",
            "Count [count]",
            )
        ) { name =>
          val (s, c) = atomicOperationTimer.getAndReset()
          (name, s, c)
        }
  }

  @inline
  def boundary[T](f: => T): T = {
    @TruffleBoundary(allowInlining = true) @noinline
    def g() = f
    g()
  }

  @inline
  private val counterCount = new LongAdder()

  def incrCounter(): Unit = if (enableCounting) boundary { counterCount.add(1) }

  @inline
  private val changeCount = new LongAdder()

  def incrChanges(): Unit = if (enableCounting) boundary { changeCount.add(1) }

  @inline
  private val initialIncrCount = new LongAdder()

  def incrInitialIncrCount(): Unit = if (enableCounting) boundary { initialIncrCount.add(1) }

  @inline
  private val flushAllCount = new LongAdder()

  def incrFlushAllCount(): Unit = if (enableCounting) boundary { flushAllCount.add(1) }

  @inline
  private val flushCount = new LongAdder()
  @inline
  private val flushSizeSum = new LongAdder()

  def incrFlushes(offset: Int): Unit = if (enableCounting) boundary {
    flushCount.add(1)
    flushSizeSum.add(Math.abs(offset))
  }

  if (enableCounting) {
    DumperRegistry.registerCSVLineDumper("counter-counts", "csv", "counter counts",
        Seq(
            "Dump ID [id]",
            "Counter Count [counters]",
            "Change Count [changes]",
            "Initial global increment count [initialIncrs]",
            "Flush all count [flushAlls]",
            "Total size of flushes [flushedSum]",
            "Flush Count [flushes]",
            )
        ) { name =>
          val counters = counterCount.sumThenReset()
          val changes = changeCount.sumThenReset()
          val flushAlls = flushAllCount.sumThenReset()
          val initialIncrs = initialIncrCount.sumThenReset()
          val flushes = flushCount.sumThenReset()
          val flushedSum = flushSizeSum.sumThenReset()
          (name, counters, changes, initialIncrs, flushAlls, flushedSum, flushes)
        }
    DumperRegistry.registerClear { () =>
      counterCount.reset()
      changeCount.reset()
      flushAllCount.reset()
      initialIncrCount.reset()
      flushCount.reset()
      flushSizeSum.reset()
    }
  }

  @sun.misc.Contended
  protected final class CounterOffset(val counter: Counter) {
    var inThreadList: Boolean = false
    var value: Int = 0
    var globalCountHeld: Boolean = false
    var nextCounterOffset: CounterOffset = null

    override def toString(): String = {
      f"CounterOffset@${hashCode()}%08x(inThreadList=$inThreadList, value=$value, globalCountHeld=$globalCountHeld)"
    }
  }

  def markCounterOffsetRemoved(headList: CounterOffset): Unit = {
    if (headList != null) {
      headList.nextCounterOffset = null
      headList.inThreadList = false
    }
  }

  /** Remove and return the first CounterOffset in the Worker.
    */
  def removeNextCounterOffset(worker: SimpleWorkStealingScheduler#Worker): Unit = {
    val headList = worker.counterOffsets.asInstanceOf[CounterOffset]
    if (headList != null) {
      worker.counterOffsets = headList.nextCounterOffset
    }
  }

  /** Remove and return the CounterOffset following `previous`.
    */
  def removeNextCounterOffset(previous: CounterOffset): Unit = {
    val headList = previous.nextCounterOffset
    if (headList != null) {
      previous.nextCounterOffset = headList.nextCounterOffset
    }
  }

  private def pushCounterOffset(worker: SimpleWorkStealingScheduler#Worker, coh: CounterOffset): Unit = {
    coh.nextCounterOffset = worker.counterOffsets.asInstanceOf[CounterOffset]
    worker.counterOffsets = coh
    coh.inThreadList = true
  }

  // TODO: In some cases there is no need to flush an all counters. Instead, flushing a known counter and all its ancestors.

  def flushAllCounterOffsets(flushPolarity: Int): Unit = {
    Thread.currentThread() match {
      case worker: SimpleWorkStealingScheduler#Worker =>
        incrFlushAllCount()
        //Logger.info(s"Flushing all ${Thread.currentThread()}: flushOnlyPositive = $flushOnlyPositive")

        var prev: CounterOffset = null
        var current = worker.counterOffsets.asInstanceOf[CounterOffset]

        while (current != null) {
          if ((flushPolarity > 0 && current.value >= 0) || (flushPolarity == 0) || (flushPolarity < 0 && current.value <= 0)) {
            //Logger.info(s"Flushing all ${Thread.currentThread()}: flushing: $current")
            if (prev != null)
              removeNextCounterOffset(prev)
            else
              removeNextCounterOffset(worker)
            markCounterOffsetRemoved(current)

            current.counter.flushCounterOffsetAndHandle(current)

            // Step to the node that replaced this one.
            if (prev != null)
              current = prev.nextCounterOffset
            else
              current = worker.counterOffsets.asInstanceOf[CounterOffset]
          } else {
            prev = current
            current = current.nextCounterOffset
          }
        }
      case _ => ()
    }
  }

  sealed trait GetCounterOffsetContext {
    def enterOffWorker(): Unit
    def profileCreateCounterOffset(b: Boolean): Boolean
    def profileInThreadList(b: Boolean): Boolean
  }

  sealed trait FlushContext extends GetCounterOffsetContext {
    def profileNonzeroOffset(b: Boolean): Boolean
    def profileHalted(b: Boolean): Boolean
  }

  sealed trait NewTokenContext extends GetCounterOffsetContext {
    def profileGlobalCountHeld(b: Boolean): Boolean
  }

  sealed class GetCounterOffsetContextImpl(runtime: PorcERuntime) extends GetCounterOffsetContext {
    def enterOffWorker() = {
      if (runtime.allExecutionOnWorkers.isValid()) {
        CompilerDirectives.transferToInterpreterAndInvalidate()
        runtime.allExecutionOnWorkers.invalidate()
      }
    }

    val createCounterOffsetProfile = ConditionProfile.createCountingProfile()
    val inThreadListProfile = ConditionProfile.createCountingProfile()

    def profileCreateCounterOffset(b: Boolean): Boolean = createCounterOffsetProfile.profile(b)
    def profileInThreadList(b: Boolean): Boolean = inThreadListProfile.profile(b)

    protected def innerString = s"createCounterOffset=$createCounterOffsetProfile, inThreadList=$inThreadListProfile"
    protected def prefixString = "GetCounterOffsetContext"
    override def toString() = s"$prefixString($innerString)"
  }

  final class NewTokenContextImpl(runtime: PorcERuntime) extends GetCounterOffsetContextImpl(runtime) with NewTokenContext {
    val globalCountHeldProfile = ConditionProfile.createCountingProfile()

    def profileGlobalCountHeld(b: Boolean): Boolean = globalCountHeldProfile.profile(b)

    protected override def innerString = s"${super.innerString}, globalCountHeld=$globalCountHeldProfile"
    protected override def prefixString = "NewTokenContext"
  }

  final class FlushContextImpl(runtime: PorcERuntime) extends GetCounterOffsetContextImpl(runtime) with FlushContext {
    val nonzeroOffsetProfile = ConditionProfile.createCountingProfile()
    val haltedProfile = ConditionProfile.createCountingProfile()

    def profileNonzeroOffset(b: Boolean): Boolean = nonzeroOffsetProfile.profile(b)
    def profileHalted(b: Boolean): Boolean = haltedProfile.profile(b)

    protected override def innerString = s"${super.innerString}, nonzeroOffset=$nonzeroOffsetProfile, halted=$haltedProfile"
    protected override def prefixString = "FlushContext"
  }

  object NoOpContext extends FlushContext with NewTokenContext {
    def enterOffWorker() = ()

    def profileCreateCounterOffset(b: Boolean): Boolean = b
    def profileInThreadList(b: Boolean): Boolean = b

    def profileNonzeroOffset(b: Boolean): Boolean = b
    def profileHalted(b: Boolean): Boolean = b

    def profileGlobalCountHeld(b: Boolean): Boolean = b
  }
}

/** A counter which tracks an executing part of the program.
  *
  * @author amp
  */
abstract class Counter protected (n: Int, val depth: Int, execution: PorcEExecution) extends AtomicInteger(n) {
  import Counter._
  import CounterConstants._

  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)
  incrCounter()

  protected def handleHaltToken(optimized: Boolean) = {
    if (false && execution.runtime.logNoninlinableSchedules) {
      if (!optimized && get() == -counterOffsets.map({
          case null => (0).toInt
          case coh => coh.value.toInt
        }).sum) {
          val myThreadOffset = Thread.currentThread() match {
            case worker: SimpleWorkStealingScheduler#Worker =>
              counterOffsets(worker.workerID)
            case _ => null
          }
          if (myThreadOffset != null /*&& myThreadOffset.value < 0 && get() == -myThreadOffset.value*/) {
            def offsetsStr = counterOffsets.collect({ case coh if coh != null => coh.value }).mkString(",")
            Logger.log(Level.WARNING, s"Effective halt: $this (${get()}; $myThreadOffset; ${offsetsStr})", new Exception)
          }
      }
    }
    SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)
  }

  if (depth > maxCounterDepth) {
    throw new StackOverflowError(s"The Orc stack is limited to $maxCounterDepth. Make sure your functions are actually tail recursive.")
  }

  def this(execution: PorcEExecution) = {
    this(1, 0, execution)
  }

  val counterOffsets = Array.ofDim[CounterOffset](execution.runtime.scheduler.maxWorkers)

  /* Multi-threaded Counter Implementation
   *
   * DESIGN:
   *
   * The AtomicInteger we derive from is the number of tasks that are either running or
   * pending. This functions similarly to a reference count and this halts when count
   * reaches 0.
   *
   * Like with reference counts, direct counting does not scale. So threads keep a map
   * of offsets which have not yet been applied to the counters. The sum of all the
   * per-thread offsets and the global count is the actual reference count. Halting is
   * detected when the global count goes to zero (the per-thread offsets cannot
   * atomically manipulated, so they cannot be used), so updates must be flushed to
   * the global count whenever a task is spawned (or a continuation is registered
   * somewhere) that will decrement a counter that was incremented in this thread.
   * To make sure halts are eventually detected the runtime needs to flush all
   * counters occasionally even if it is not required.
   *
   * Due to limitations in the design of Porc(E) it is impossible to know what counters
   * a continuation references and which may be decremented, so the program must flush
   * all counters when any closure is scheduled.
   *
   * A thread can safely operate in a counter using only its local offset if the counter
   * cannot halt. So, each thread must have at least one global count (token) if any
   * tasks on the thread have incremented the count. Threads, therefore, keep a flag
   * specifying if the thread currently has a global count (token). If the flag is not
   * set when an offset is being incremented then the global count is incremented
   * instead and the flag is set. The flag is only cleared during flushing; this adds
   * an additional cost to flushing since it will force the counter to perform an
   * atomic increment do set the flag again.
   *
   * The implemented uses a number of optimizations:
   *  1. The flushes forced by spawned tasks only need to flush counters with a positive
   *     offset. Those with negative offset can only trigger halts and cannot create
   *     erroneous halting, so they can be left in the local offset to avoid atomic
   *     operations.
   *  2. The full flushes (including negative offsets) execute only after a thread runs
   *     out of work and fails to steal more.
   */

  /* NOTES: (Not edited; Arthur wrote this; ask me about it if needed.)
   *
   * For each thread:
   *
   * If the offset is positive, the thread has more local tasks (nested on the stack)
   * than are represented in the global count. The thread must hold at least one global
   * count for this to be safe.
   *
   * If the offset is zero, the thread has exactly the same number of tasks as it has tokens
   * in the global count. This does *not* imply that the threads task count is zero. This
   * state is safe since all local tasks are represented as global counts so erroneous
   * halting cannot happen.
   *
   * If the offset is negative, the thread has fewer active tasks than those represented
   * in the global count. This state is always safe since the global count cannot be zero
   * and halting cannot occur.
   *
   * ---
   *
   * The tricky part here is the transitions between states. The offset does not provide
   * enough information to know for sure whether the thread has excess global counts.
   * However, determining the local count exactly would require providing the counter
   * of a task directly and that is not the case at the moment. However it is possible
   * to detect when a thread has at least one global count if it performs at least one
   * global increment. This could then set a flag that shows that the current thread
   * has *at least* one global count. It might have more, but it means that positive
   * offset is safe.
   *
   * The challenge of this encoding is deciding when to flush the offset out to global
   * counter. This flush needs to happen for halt detection to occur, but it needs to
   * happen as little as possible to avoid overhead.
   *
   * * If the counters are stored in the thread, then whenever a thread returns to the
   *   scheduler all counters could be flushed. This would be simple, but for cases
   *   where there are lots of small tasks in the system it would result in frequent
   *   flushes. This could be improved by only flushing when stealing or only flushing
   *   at most every so often (1ms or something).
   */

  @TruffleBoundary(allowInlining = true) @noinline
  protected final def scheduleHaltClosureIfNeeded(c: PorcEClosure): Unit = {
    if (c != null) {
      val s = CallClosureSchedulable(c, execution)
      SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
      execution.runtime.potentiallySchedule(s)
    }
  }

  protected final def getCounterOffset()(implicit ctx: GetCounterOffsetContext): CounterOffset = {
    val thread = Thread.currentThread()
    if (thread.isInstanceOf[SimpleWorkStealingScheduler#Worker]) {
      val worker = thread.asInstanceOf[SimpleWorkStealingScheduler#Worker]
      if (ctx.profileCreateCounterOffset(counterOffsets(worker.workerID) == null)) {
        @TruffleBoundary(allowInlining = false) @noinline
        def newCOH() = {
          val r = new CounterOffset(this)
          counterOffsets(worker.workerID) = r
          pushCounterOffset(worker, r)
          r
        }
        newCOH()
      } else {
        val offset = counterOffsets(worker.workerID)
        if (!ctx.profileInThreadList(offset.inThreadList)) {
          pushCounterOffset(worker, offset)
        }
        offset
      }
    } else {
      ctx.enterOffWorker();
      null
    }
  }

  private final def flushCounterOffsetAndGet(coh: CounterOffset): Int = {
    val n = addAndGet(coh.value)
    incrFlushes(coh.value)
    //Logger.info(s"Flushed $this ($coh) @ ${Thread.currentThread()}: global before = ${n - coh.value}, global after = ${n}")
    coh.value = 0
    coh.globalCountHeld = false
    n
  }

  final def flushCounterOffsetAndHandleOptimized(coh: CounterOffset)(implicit ctx: FlushContext): PorcEClosure = {
    if (ctx.profileNonzeroOffset(coh.value != 0)) {
      val n = flushCounterOffsetAndGet(coh)
      if (ctx.profileHalted(n == 0)) {
        onHaltOptimized()
      } else {
        null
      }
    } else {
      coh.globalCountHeld = false
      null
    }
  }

  @TruffleBoundary(allowInlining = true) @noinline
  final def flushCounterOffsetAndHandle(coh: CounterOffset): Unit = {
    scheduleHaltClosureIfNeeded(flushCounterOffsetAndHandleOptimized(coh)(NoOpContext))
  }

  @volatile
  var isDiscorporated = false

  final def setDiscorporate() = {
    //assert(get() > 0)
    isDiscorporated = true
  }

  final def discorporateToken() = {
    setDiscorporate()
    haltToken()
  }

  /** Decrement the count and check for overall halting.
    *
    * If we did halt call onContextHalted().
    */
  final def haltToken(): Unit = {
    scheduleHaltClosureIfNeeded(haltTokenOptimized(NoOpContext))
  }

  final def haltTokenOptimized(ctx: GetCounterOffsetContext): PorcEClosure = {
    implicit val _ctx = ctx
    val s = startTimer()
    val coh = getCounterOffset()
    incrChanges()

    @TruffleBoundary(allowInlining = false) @noinline
    def decrGlobal(): PorcEClosure = {
      val n = decrementAndGet()
      handleHaltToken(n == 0)
      if (tracingEnabled) {
        assert(n >= 0, s"Halt is not allowed on already stopped Counters: $this")
      }
      if (n == 0) {
        onHaltOptimized()
      } else {
        null
      }
    }

    val r = if (CompilerDirectives.injectBranchProbability(CompilerDirectives.FASTPATH_PROBABILITY,
        coh != null)) {
      coh.value -= 1
      //handleHaltToken(false)
      null
    } else {
      ctx.enterOffWorker()
      decrGlobal()
    }
    stopTimer(s)
    r
  }

  /** Increment the count.
    */
  final def newToken(): Unit = {
    if (newTokenOptimized(NoOpContext)) {
      doResurrect()
    }
  }

  final def newTokenOptimized(ctx: NewTokenContext): Boolean = {
    val s = startTimer()
    val coh = getCounterOffset()(ctx)
    incrChanges()

    @TruffleBoundary(allowInlining = false) @noinline
    def incrGlobal(coh: CounterOffset) = {
      if (coh != null) {
        incrInitialIncrCount()
        //Logger.info(s"First count $this ($coh) @ ${Thread.currentThread()}: global before = ${get()}")
        coh.globalCountHeld = true
      }
      val n = getAndIncrement()
      if (tracingEnabled) {
        assert(n >= 0, s"Spawning is not allowed once we go to zero count. $this")
      }
      n == 0
    }

    val r = if (coh != null && ctx.profileGlobalCountHeld(coh.globalCountHeld)) {
      coh.value += 1
      false
    } else {
      incrGlobal(coh)
    }
    stopTimer(s)
    r
  }

  @TruffleBoundary(allowInlining = false) @noinline
  final def doResurrect() = {
    if (tracingEnabled) {
      Logger.fine(s"Resurrected $this")
      //assert(isDiscorporated, s"Resurrected counters must be discorporated. $this")
    }
    onResurrect()
  }

  override def toString(): String = {
    val n = getClass.getSimpleName
    val h = System.identityHashCode(this)
    f"$n%s@$h%08x"
  }

  /** Called when this whole context has halted.
    *
    * This needs to be thread-safe.
    */
  def onHaltOptimized(): PorcEClosure

  /** Called when this context is resurrected and becomes alive again.
    *
    * This counter is being resurrected, so we need a new token from the parent.
    *
    * This needs to be thread-safe.
    */
  def onResurrect(): Unit
}

// FIXME: There is a race that could potentially cause a counter to be non-zero without holding a token in it's parent.
//   This could only happen if a counter is resurrecting and is delayed for a long time between increasing it's counter
//   and calling parent.newToken(). See "SNZI: Scalable NonZero Indicators", Faith Ellen, et al. fig 4 for more details.
//   This may or may not affect Orc because there is only one case where Orc should call newToken on a counter
//   on which it does not have an existing token: resurrecting a service. However, services are discorporated so
//   0-transients are safe as long as there is a token somewhere else in the program to keep the runtime from exiting.
//   This token should always exist since some other part of the program must be trying to call the service and there
//   for have a token.

/** A Counter which forwards it's halting to a parent Counter and executes a closure on halt.
  *
  */
final class CounterNested(execution: PorcEExecution, val parent: Counter, haltContinuation: PorcEClosure)
    extends Counter(1, parent.depth + 1, execution) {
  import CounterConstants._

  //Tracer.trace(Counter.CounterNestedCreated, hashCode(), parent.hashCode(), 0)

  if (tracingEnabled) {
    require(execution != null)
    require(parent != null)
    require(haltContinuation != null)
  }

  def onResurrect() = {
    if (tracingEnabled) {
      assert(isDiscorporated)
    }
    parent.newToken()
  }

  // TODO: Fix duplication between onHalt and haltTokenOptimized.

  def onHaltOptimized(): PorcEClosure = {
    // Call the haltContinuation if we didn't discorporate.
    if (!isDiscorporated) {
      //Logger.info(s"$haltContinuation")
      // Token: from parent
      haltContinuation
    } else {
      parent.setDiscorporate()
      // Token: from parent
      parent.haltToken()
      null
    }
  }
}

// FIXME: MEMORYLEAK: Null all fields when the counter is halted but not discorporated.

/** A counter which always stays alive long enough to detect kills.
  *
  * This is specifically designed to handle calls into a Service methods.
  *
  */
final class CounterService(execution: PorcEExecution, val parentCalling: Counter, val parentContaining: Counter, terminator: Terminator)
    extends Counter(1, parentCalling.depth+1, execution) with Terminatable {
  //Tracer.trace(Counter.CounterServiceCreated, hashCode(), parentCalling.hashCode(), parentContaining.hashCode())

  if (CounterConstants.tracingEnabled) {
    require(execution != null)
    require(terminator != null)
    require(parentCalling != null)
    require(parentContaining != null)
  }

  /** True if this counter has tokens from it's parents.
    */
  private val haveTokens = new AtomicBoolean(true)

  init()

  private def init() = {
    terminator.addChild(this)
    // Token: We were not passed a token for parentContaining so we need to get one.
    parentContaining.newToken()
  }

  def onResurrect() = {
    if (haveTokens.compareAndSet(false, true)) {
      parentCalling.newToken()
      init()
    }
  }

  private def haltParentToken() = {
    scheduleHaltClosureIfNeeded(haltParentTokenOptimized())
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private def haltParentTokenOptimized(): PorcEClosure = {
    if (haveTokens.compareAndSet(true, false)) {
      terminator.removeChild(this)
      // Token: We were passed a token at creation
      parentCalling.haltToken()
      // Token: We created a new token in the constructor.
      parentContaining.haltTokenOptimized(Counter.NoOpContext)
    } else {
      null
    }
  }

  def onHaltOptimized(): PorcEClosure = {
    if (!isDiscorporated) {
      haltParentTokenOptimized()
    } else {
      // If we would discorporate instead just wait and see if a kill comes.
      null
    }
  }

  def kill() = {
    haltParentToken()
  }
}

/** A counter which kills the terminator when it halts.
  *
  * This is specifically designed to make sure terminators do not become garbage when there body halts.
  *
  */
final class CounterTerminator(execution: PorcEExecution, val parent: Counter, terminator: Terminator)
    extends Counter(1, parent.depth+1, execution) with Terminatable {
  //Tracer.trace(Counter.CounterTerminatorCreated, hashCode(), parent.hashCode(), terminator.hashCode())

  if (CounterConstants.tracingEnabled) {
    require(execution != null)
    require(terminator != null)
    require(parent != null)
  }

  /** True if this counter has tokens from it's parents.
    */
  private val state = new AtomicInteger(CounterTerminator.HasTokens)

  init()

  private def init() = {
    terminator.addChild(this)
  }

  def onResurrect() = {
    if (state.compareAndSet(CounterTerminator.HasNoTokens, CounterTerminator.HasTokens)) {
      parent.newToken()
      init()
    }
  }

  def onHaltOptimized(): PorcEClosure = {
    if (isDiscorporated) {
      if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.HasNoTokens)) {
        parent.discorporateToken()
      }
      null
    } else {
      if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.WasKilled)) {
        val r = parent.haltTokenOptimized(Counter.NoOpContext)
        try {
          // Just make sure terminator is killed. If it already was ignore the exception.
          terminator.kill()
        } catch {
          case _: KilledException =>
            ()
        }
        r
      } else {
        null
      }
    }
  }

  def kill() = {
    if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.WasKilled)) {
      // Do nothing.
      // The token we have is passed to the invoker of terminator.kill()
    }
  }
}

object CounterTerminator {
  @inline
  val HasNoTokens = 0
  @inline
  val HasTokens = 1
  @inline
  val WasKilled = 2
}
