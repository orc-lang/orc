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

import java.io.{ PrintWriter, StringWriter }
import java.util.concurrent.{ ConcurrentHashMap, LinkedBlockingDeque }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.logging.Level

import scala.annotation.elidable

import orc.ast.porc
import orc.run.porce.{ HasPorcNode, Logger, SimpleWorkStealingSchedulerWrapper }
import orc.run.extensions.SimpleWorkStealingScheduler
import orc.util.Tracer

import com.oracle.truffle.api.{ RootCallTarget, Truffle }
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.FrameInstance
import com.oracle.truffle.api.nodes.{ Node => TruffleNode }
import orc.util.SummingStopWatch
import orc.util.DumperRegistry
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal
import java.util.IdentityHashMap
import java.util.concurrent.atomic.LongAdder
import com.oracle.truffle.api.CompilerDirectives

@CompilationFinal
object Counter {
  import CounterConstants._

  val liveCounters = ConcurrentHashMap.newKeySet[Counter]()

  def exceptionString(e: Exception) = {
    val ss = new StringWriter()
    e.printStackTrace(new PrintWriter(ss))
    val s = ss.toString()
    s.dropWhile(_ != ':').drop(1)
  }

  @TruffleBoundary @noinline
  def getPorcStackTrace(): Seq[Either[porc.PorcAST, String]] = {
    val b = Seq.newBuilder[Either[porc.PorcAST, String]]
    def appendIfPorc(n: TruffleNode) = n match {
      case n: HasPorcNode =>
        n.porcNode match {
          case Some(n) =>
            b += Left(n)
          case None =>
            b += Right(n.toString())
        }
      case null =>
        {}
      case n =>
        b += Right(n.toString())
    }
    Truffle.getRuntime.iterateFrames((f: FrameInstance) => {
      appendIfPorc(f.getCallNode)
      f.getCallTarget match {
        case c: RootCallTarget =>
          appendIfPorc(c.getRootNode)
        case t =>
          Logger.fine(s"Found unknown target: $t")
      }
    })
    b.result()
  }

  val leavesOnly = false

  @TruffleBoundary @noinline
  def report() = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      import scala.collection.JavaConverters._

      val allCounters = liveCounters.asScala
      lazy val parentCounters = allCounters.collect({
        case c: CounterNested =>
          c.parent
      }).toSet
      lazy val leafCounters = allCounters.filterNot(parentCounters)
      val counters = if (leavesOnly) leafCounters else allCounters

      Logger.fine(s"========================= Counter Report; showing ${counters.size} of ${allCounters.size} counters")
      Logger.fine("\n" + counters.map(c => s"$c: log size = ${c.log.size}, count = ${c.get}, isDiscorporated = ${c.isDiscorporated}").mkString("\n"))
      for (c <- counters) {
        if(true || c.get > 0) {
          Logger.fine(s"$c:")
          for(e <- c.log.asScala) {
            Logger.fine(exceptionString(e))
          }
          c.log.clear()
        }
      }
    } else {
      //Logger.warning(s"Cannot report Counter information if FINE is not loggable in ${Logger.julLogger.getName} or tracingEnabled == false")
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary @noinline
  def addCounter(c: Counter) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.add(c)
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary @noinline
  def removeCounter(c: Counter) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.remove(c)
    }
  }

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

  protected class CounterOffsetHolder() {
    var value: Int = 0
    var globalCountHeld: Boolean = false
  }
  private val counterOffsetsThreadLocal = new ThreadLocal[IdentityHashMap[Counter, CounterOffsetHolder]]() {
    override def initialValue() = {
      //Logger.info(s"Counter offsets initializing for ${Thread.currentThread()}")
      new IdentityHashMap[Counter, CounterOffsetHolder]()
    }
  }

  @TruffleBoundary(allowInlining = false) @noinline
  private def getCounterOffsetHolder(c: Counter): CounterOffsetHolder = {
    if (Thread.currentThread().isInstanceOf[SimpleWorkStealingScheduler#Worker]){
      val map = counterOffsetsThreadLocal.get()
      val coh = map.get(c)
      if (coh == null) {
        val coh1 = new CounterOffsetHolder()
        map.put(c, coh1)
        coh1
      } else {
        coh
      }
    } else {
      //Logger.info(s"Non-worker CounterOffsetHolder request on ${Thread.currentThread()}")
      null
    }
  }

  def flushAllCounterOffsets(flushOnlyPositive: Boolean = false): Unit = if (Thread.currentThread().isInstanceOf[SimpleWorkStealingScheduler#Worker]){
    import scala.collection.JavaConverters._
    val map = counterOffsetsThreadLocal.get()

    incrFlushAllCount()
    // TODO: PERFORMANCE: This does lots of iterations and repeated checks. It should be optimized.
    while(!map.isEmpty() && (!flushOnlyPositive || map.values().asScala.exists(_.value > 0))) {
      val elements = map.asScala.toArray
      elements.filter(e => !flushOnlyPositive || e._2.value > 0).foreach(e => map.remove(e._1))
      for ((c, coh) <- elements) {
        if (!flushOnlyPositive || coh.value > 0)
          c.flushCounterOffsetAndHandle(coh)
      }
    }
  }
}

/** A counter which tracks an executing part of the program.
  *
  * @author amp
  */
abstract class Counter protected (n: Int, val depth: Int, execution: PorcEExecution) extends AtomicInteger(n) {
  import CounterConstants._
  import Counter._

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

  @elidable(elidable.ASSERTION)
  private val log = if (tracingEnabled) new LinkedBlockingDeque[Exception]() else null

  @elidable(elidable.ASSERTION)
  @TruffleBoundary @noinline
  protected final def logChange(s: => String) = {
    //Logger.finer(s"Counter $this: $s")
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      val stack = Counter.getPorcStackTrace().map(n => {
        n match {
          case Left(n) =>
            def rangeStr = n.sourceTextRange.map(_.lineContentWithCaret).getOrElse("")
            def nodeStr = n.toString().take(80)
            s"$rangeStr\n$nodeStr"
          case Right(s) =>
            s"[$s]"
        }
      }).mkString("\n---vvv---\n")
      log.add(new Exception(s"$s in Porc stack:\n$stack"))
      if(log.size() > 1000) {
        log.pollFirst()
      }
    }
  }

  if (tracingEnabled) {
    logChange(s"Init to $n")
    Counter.addCounter(this)
  }

  // */

  /*
  val log: LinkedBlockingDeque[Exception] = null
  @inline
  private def logChange(s: => String) = {
  }
  // */

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

  protected def getCounterOffsetHolder(): CounterOffsetHolder = Counter.getCounterOffsetHolder(this)

  def flushCounterOffsetAndHandle(coh: CounterOffsetHolder): Unit = {
    def flushCounterOffsetAndGet(coh: CounterOffsetHolder): Int = {
      val n = addAndGet(coh.value)
      incrFlushes(coh.value)
      //Logger.info(s"Flushed $this ($coh) @ ${Thread.currentThread()}: offset = ${coh.value}, global before = ${n - coh.value}, global after = ${n}")
      coh.value = 0
      coh.globalCountHeld = false
      n
    }
    if (coh.value != 0) {
      val n = flushCounterOffsetAndGet(coh)
      if (n == 0) {
        doHalt()
      }
    }
  }

  def flushCounterOffsetAndHandle(): Unit = flushCounterOffsetAndHandle(getCounterOffsetHolder())

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
    val coh = getCounterOffsetHolder()
    incrChanges()

    @TruffleBoundary(allowInlining = true) @noinline
    def decrGlobal() = {
      val n = decrementAndGet()
      handleHaltToken()
      if (n == 0) {
        doHalt()
      }
    }

    if (CompilerDirectives.injectBranchProbability(
        CompilerDirectives.FASTPATH_PROBABILITY,
        coh != null)) {
      coh.value -= 1
    } else {
      decrGlobal()
    }
    stopTimer(s)
    /*
    if (tracingEnabled) {
      logChange(s"- Down to $n")
      if (n < 0) {
        Counter.report()
      }
      assert(n >= 0, s"Halt is not allowed on already stopped Counters: $this")
    }
    */
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private final def doHalt() = {
    if (tracingEnabled) {
      //Logger.fine(s"Halted $this")
      //Counter.report()
      if (!isDiscorporated) {
        //Counter.removeCounter(this)
      }
    }
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
    val coh = getCounterOffsetHolder()
    incrChanges()

    @TruffleBoundary(allowInlining = true) @noinline
    def incrGlobal() = {
      val n = getAndIncrement()
      if (tracingEnabled) {
        logChange(s"+ Up from $n")
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
        incrInitialIncrCount()
        coh.globalCountHeld = true
        //Logger.info(s"First count $this ($coh) @ ${Thread.currentThread()}: offset = ${coh.value}, global before = ${n - 1}, global after = ${n}")
        incrGlobal()
      }
    } else {
      incrGlobal()
    }
    stopTimer(s)
    r
  }

  @TruffleBoundary(allowInlining = false) @noinline
  final def doResurrect() = {
    if (tracingEnabled) {
      Counter.addCounter(this)
      Logger.fine(s"Resurrected $this")
      //if(!isDiscorporated) Counter.report()
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
  import CounterConstants._
  import Counter._

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
    val coh = getCounterOffsetHolder()
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
