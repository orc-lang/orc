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
import scala.collection.mutable.ArrayBuffer

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
  private var counterCount = new LongAdder()

  @TruffleBoundary(allowInlining = true) @noinline
  def incrCounter(): Unit = if (enableCounting) counterCount.add(1)

  @inline
  private var changeCount = new LongAdder()

  @TruffleBoundary(allowInlining = true) @noinline
  def incrChanges(): Unit = if (enableCounting) changeCount.add(1)

  @inline
  private var initialIncrCount = new LongAdder()

  @TruffleBoundary(allowInlining = true) @noinline
  def incrInitialIncrCount(): Unit = if (enableCounting) initialIncrCount.add(1)

  @inline
  private var flushAllCount = new LongAdder()

  @TruffleBoundary(allowInlining = true) @noinline
  def incrFlushAllCount(): Unit = if (enableCounting) flushAllCount.add(1)

  @inline
  private var flushCount = new LongAdder()
  @inline
  private var flushSizeSum = new LongAdder()


  @TruffleBoundary(allowInlining = true) @noinline
  def incrFlushes(offset: Int): Unit = if (enableCounting) {
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

  protected class CounterOffset(val counter: Counter) {
    var inThreadList: Boolean = false
    var value: Int = 0
    var globalCountHeld: Boolean = false
    var nextCounterOffset: CounterOffset = null

    def register(worker: SimpleWorkStealingScheduler#Worker) = {
      if (CompilerDirectives.injectBranchProbability(
        CompilerDirectives.SLOWPATH_PROBABILITY,
        !inThreadList)) {
        // CompilerDirectives.transferToInterpreter()
        nextCounterOffset = worker.counterOffsets.asInstanceOf[CounterOffset]
        worker.counterOffsets = this
        inThreadList = true
      }
    }

    override def toString(): String = {
      f"CounterOffset@${hashCode()}%x(inThreadList=$inThreadList, value=$value, globalCountHeld=$globalCountHeld)"
    }
  }

  def flushAllCounterOffsets(flushOnlyPositive: Boolean = false): Unit = {
    Thread.currentThread() match {
      case worker: SimpleWorkStealingScheduler#Worker =>
        incrFlushAllCount()
        //Logger.info(s"Flushing all ${Thread.currentThread()}: flushOnlyPositive = $flushOnlyPositive")
        var done = false
        while(!done) {
          done = true
          val headList = worker.counterOffsets.asInstanceOf[CounterOffset]
          worker.counterOffsets = null

          if (headList != null) {
            //Logger.info(s"Flushing all ${Thread.currentThread()}: flushOnlyPositive = $flushOnlyPositive: BEFORE\n${Stream.iterate(headList)(_.nextCounterOffset).takeWhile(_ != null).mkString("\n")}")
          }

          val buffer = ArrayBuffer[CounterOffset]()

          {
            var current = headList
            while (current != null) {
              current.inThreadList = false
              buffer += current
              current = current.nextCounterOffset
            }
          }

          for (current <- buffer) {
            if (!flushOnlyPositive || current.value >= 0) {
              //Logger.info(s"Flushing all ${Thread.currentThread()}: flushing: $current")
              current.counter.flushCounterOffsetAndHandle(current)
              done = false
            } else {
              //Logger.info(s"Flushing all ${Thread.currentThread()}: adding back into the queue: $current")
              current.register(worker)
            }
          }

          if (worker.counterOffsets != null) {
            //Logger.info(s"Flushing all ${Thread.currentThread()}: flushOnlyPositive = $flushOnlyPositive: AFTER\n${Stream.iterate(worker.counterOffsets.asInstanceOf[CounterOffset])(_.nextCounterOffset).takeWhile(_ != null).mkString("\n")}")
          }
        }
      case _ => ()
    }
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

  protected def handleHaltToken() = {
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

  protected def getCounterOffset(): CounterOffset = {
    Thread.currentThread() match {
      case worker: SimpleWorkStealingScheduler#Worker =>
        if (counterOffsets(worker.workerID) == null) {
          //CompilerDirectives.transferToInterpreter()
          val r = new CounterOffset(this)
          counterOffsets(worker.workerID) = r
        }
        val offset = counterOffsets(worker.workerID)
        offset.register(worker)
        offset
      case _ => null
    }
  }

  def flushCounterOffsetAndHandle(coh: CounterOffset): Unit = {
    def flushCounterOffsetAndGet(coh: CounterOffset): Int = {
      val n = addAndGet(coh.value)
      incrFlushes(coh.value)
      //Logger.info(s"Flushed $this ($coh) @ ${Thread.currentThread()}: global before = ${n - coh.value}, global after = ${n}")
      coh.value = 0
      coh.globalCountHeld = false
      n
    }
    if (coh.value != 0) {
      val n = flushCounterOffsetAndGet(coh)
      if (n == 0) {
        doHalt()
      }
    } else {
      coh.globalCountHeld = false
    }
  }

  def flushCounterOffsetAndHandle(): Unit = flushCounterOffsetAndHandle(getCounterOffset())

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
    val s = startTimer()
    val coh = getCounterOffset()
    incrChanges()

    @TruffleBoundary(allowInlining = true) @noinline
    def decrGlobal() = {
      val n = decrementAndGet()
      handleHaltToken()
      if (tracingEnabled) {
        assert(n >= 0, s"Halt is not allowed on already stopped Counters: $this")
      }
      if (n == 0) {
        doHalt()
      }
    }

    if (CompilerDirectives.injectBranchProbability(
        CompilerDirectives.FASTPATH_PROBABILITY,
        coh != null)) {
      coh.value -= 1
    } else {
      //CompilerDirectives.transferToInterpreter()
      decrGlobal()
    }
    stopTimer(s)
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private final def doHalt() = {
    onHalt()
  }

  /** Increment the count.
    */
  final def newToken(): Unit = {
    if (newTokenOptimized()) {
      doResurrect()
    }
  }

  final def newTokenOptimized(): Boolean = {
    val s = startTimer()
    val coh = getCounterOffset()
    incrChanges()

    @TruffleBoundary(allowInlining = true) @noinline
    def incrGlobal() = {
      val n = getAndIncrement()
      if (tracingEnabled) {
        assert(n >= 0, s"Spawning is not allowed once we go to zero count. $this")
      }
      n == 0
    }

    val r = if (CompilerDirectives.injectBranchProbability(
        CompilerDirectives.FASTPATH_PROBABILITY,
        coh != null)) {
      if (coh.globalCountHeld) {
        coh.value += 1
        false
      } else {
        //CompilerDirectives.transferToInterpreter()
        incrInitialIncrCount()
        //Logger.info(s"First count $this ($coh) @ ${Thread.currentThread()}: global before = ${get()}")
        coh.globalCountHeld = true
        incrGlobal()
      }
    } else {
      //CompilerDirectives.transferToInterpreter()
      incrGlobal()
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
    f"$n%s@$h%x"
  }

  /** Called when this whole context has halted.
    *
    * This needs to be thread-safe.
    */
  def onHalt(): Unit

  /** Called when this context is resurrected and becomes alive again.
    *
    * This counter is being resurrected, so we need a new token from the parent.
    *
    * This needs to be thread-safe.
    */
  def onResurrect(): Unit
}

/** A Counter which forwards it's halting to a parent Counter and executes a closure on halt.
  *
  */
final class CounterNested(execution: PorcEExecution, val parent: Counter, haltContinuation: PorcEClosure)
    extends Counter(1, parent.depth + 1, execution) {
  import Counter._
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

  def onHalt(): Unit = {
    // Call the haltContinuation if we didn't discorporate.
    if (!isDiscorporated) {
      // Token: from parent
      /* ROOTNODE-STATISTICS
      haltContinuation.body.getRootNode() match {
        case n: PorcERootNode => n.incrementHalt()
        case _ => ()
      }
      */
      val s = CallClosureSchedulable(haltContinuation, execution)
      SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
      execution.runtime.potentiallySchedule(s)
    } else {
      parent.setDiscorporate()
      // Token: from parent
      parent.haltToken()
    }
  }

  def haltTokenOptimized(): PorcEClosure = {
    // DUPLICATED: from Counter.haltToken()
    val s = startTimer()
    val coh = getCounterOffset()
    incrChanges()

    @TruffleBoundary(allowInlining = true) @noinline
    def decrGlobal() = {
      val n = decrementAndGet()
      handleHaltToken()
      if (n == 0) {
        // Call the haltContinuation if we didn't discorporate.
        if (!isDiscorporated) {
          // Token: from parent
          haltContinuation
        } else {
          parent.setDiscorporate()
          // Token: from parent
          parent.haltToken()
          null
        }
      } else {
        null
      }
    }

    val r = if (CompilerDirectives.injectBranchProbability(
        CompilerDirectives.FASTPATH_PROBABILITY,
        coh != null)) {
      coh.value -= 1
      null
      // TODO: PERFORMANCE: Always returning null disables inlining of halt closures into the halt token operation.
      /*
       * If we compare the global counter to the offset on every decrement, then if the
       * offset - the global count = 0 then we can flush and potentially go to zero.
       * This approach requires lots of reads from the global count which could cause
       * cache contention if the counter is written frequently. In addition this approach
       * is not complete: Multiple threads could end up waiting with portions of the
       * tokens and neither would flush since neither has all of them.
       *
       * Cache bouncing reads could be avoided by marking counters which are used in multiple
       * threads and disabling the check for them.
       */
    } else {
      decrGlobal()
    }
    stopTimer(s)
    r
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
    if (haveTokens.compareAndSet(true, false)) {
      terminator.removeChild(this)
      // Token: We were passed a token at creation
      parentCalling.haltToken()
      // Token: We created a new token in the constructor.
      parentContaining.haltToken()
    }
  }

  def onHalt(): Unit = {
    if (!isDiscorporated) {
      haltParentToken()
    } else {
      // If we would discorporate instead just wait and see if a kill comes.
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

  def onHalt(): Unit = {
    if (isDiscorporated) {
      if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.HasNoTokens)) {
        parent.discorporateToken()
      }
    } else {
      if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.WasKilled)) {
        parent.haltToken()
        try {
          // Just make sure terminator is killed. If it already was ignore the exception.
          terminator.kill()
        } catch {
          case _: KilledException =>
            ()
        }
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
