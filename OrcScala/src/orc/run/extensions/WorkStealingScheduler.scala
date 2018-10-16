//
// WorkStealingScheduler.scala -- A work-stealing scheduler for Orc
// Project OrcScala
//
// Created by amp on Feb, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level

import scala.collection.mutable.ArrayBuffer

import orc.{ OrcExecutionOptions, Schedulable }
import orc.run.Orc
import orc.util.ABPWSDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.Collections
import java.util.WeakHashMap
import orc.util.DumperRegistry
import orc.util.SchedulingQueue
import orc.run.StopWatches
import orc.Schedulable
import orc.util.ExecutionLogOutputStream
import java.io.OutputStreamWriter
import orc.util.CsvWriter

/** @param monitorInterval The interval at which the monitor thread runs and checks that the thread pool is the correct size.
  * @param goalExtraThreads The ideal number of extra idle threads that the pool should contain.
  * @param workerQueueLength The length of the queues maintained by the threads. If this is too small the threads will overflow their queues frequently, if this is too large it will waste memory.
  *
  * @author amp
  */
class SimpleWorkStealingScheduler(
    maxSiteThreads: Int,
    val monitorInterval: Int = 100,
    val goalExtraThreads: Int = 0,
    val workerQueueLength: Int = 1024 * 2) {
  schedulerThis =>

  val nCores = Runtime.getRuntime().availableProcessors()

  val overrideWorkers = Option(System.getProperty("orc.SimpleWorkStealingScheduler.overrideWorkers")).map(_.toInt)
  overrideWorkers foreach { n =>
    Logger.info(s"Worker count fixed at $n (read from System property orc.SimpleWorkStealingScheduler.overrideWorkers).")
  }

  /** The minimum number of worker threads.
    */
  val minWorkers = overrideWorkers.getOrElse(math.max(4, nCores * 1))
  /** The maximum number of worker threads.
    */
  val maxWorkers = overrideWorkers.getOrElse(minWorkers + maxSiteThreads)
  /** The maximum amount of time (ms) to wait between attempts to steal work.
    */
  val maxStealWait = 10
  /** The ideal number of active (non-blocked) worker threads.
    */
  val goalUsableThreads = minWorkers + goalExtraThreads
  /** The maximum number of active we should have.
    */
  val maxUsableThreads = (goalUsableThreads * 1.1 + 0.5).toInt
  /** The interval (ms) between thread statistics dumps.
    */
  val dumpInterval = -1 // 10000
  /** The number of elements to evict from the local queue when it overflows.
    *
    * This number should be fairly large to amortize the cost of eviction.
    */
  val itemsToEvictOnOverflow = workerQueueLength / 2

  /** The time (ms) that the pool must be underprovisioned before it should add a worker thread.
    */
  val underprovisioningGracePeriod: Int = 2 * 1000
  /** The time (ms) that the pool must be overprovisioned before it should remove a worker thread.
    */
  val overprovisioningGracePeriod: Int = 10 * 1000

  /** The minimum amount of time (ms) between adding one new thread and adding the next new thread.
    */
  val threadAddMinPeriod: Int = 250
  /** The minimum amount of time (ms) between removing one thread and removing another.
    *
    * This is value may be undershot slightly due to how this is implemented.
    */
  val threadRemoveMinPeriod: Int = 250

  /** The average period of taking new work from another queue even if this thread has work.
    *
    */
  val newWorkPeriod = -1 // 100000

  require(maxSiteThreads >= 0)
  require(monitorInterval >= 0)
  require(goalExtraThreads >= 0)
  require(workerQueueLength > 4)

  assert(minWorkers <= maxWorkers)
  assert(itemsToEvictOnOverflow > 0)

  private val workers = new Array[Worker](maxWorkers)
  @volatile
  private var nWorkers = 0
  private var nextWorkerID = 0

  private def currentWorkers = workers.view(0, nWorkers)

  private val monitor = new Monitor()

  private val inputQueue = new ConcurrentLinkedQueue[Schedulable]()
  private var inputTasks = 0

  @inline
  final def potentiallyBlocking[T](f: => T): T = {
    // Allow failure by class cast exception.
    Thread.currentThread().asInstanceOf[Worker].potentiallyBlocking(f)
  }

  @volatile
  private var isSchedulerShuttingDown = false

  class Monitor extends Thread("Monitor") {
    override def run() = {
      var lastDumpTime = System.currentTimeMillis()
      var beginTimeOverprovisioned = lastDumpTime
      var beginTimeUnderprovisioned = lastDumpTime
      while (!isSchedulerShuttingDown) {
        val ws = currentWorkers
        val nw = ws.size

        val nBlocked = ws.count(t => {
          val state = t.getState
          val isBlocked = if (t.isInternallyBlocked) {
            false
          } else if (t.isPotentiallyBlocked) {
            state != Thread.State.RUNNABLE
          } else {
            //state != Thread.State.RUNNABLE && state != Thread.State.BLOCKED && state != Thread.State.NEW
            false
          }
          //Logger.finest(s"Examined thread $t: $state = $isBlocked (${t.isInternallyBlocked}, ${t.isPotentiallyBlocked})")
          isBlocked
        })

        val currentTime = System.currentTimeMillis()
        val nUsableWorkers = nw - nBlocked

        if (nUsableWorkers < goalUsableThreads && nWorkers < maxWorkers) {
          // If we have been overprovisioned by at least the grace period start killing threads.
          if (currentTime - beginTimeUnderprovisioned > underprovisioningGracePeriod) {
            Logger.fine(s"Starting new worker due to: nWorkers = $nw nUsableWorkers=$nUsableWorkers goalUsableThreads=$goalUsableThreads nBlocked=$nBlocked")
            addWorker()
            // Step the beginTimeOverprovisioned forward by an amount so we don't start a lot of threads all at once.
            beginTimeUnderprovisioned += threadAddMinPeriod
          }
        } else {
          // If we are not overprovisioned reset the beginTime for it.
          beginTimeUnderprovisioned = currentTime
        }

        if (nUsableWorkers > maxUsableThreads && nWorkers > minWorkers) {
          // If we have been overprovisioned by at least the grace period start killing threads.
          if (currentTime - beginTimeOverprovisioned > overprovisioningGracePeriod) {
            schedulerThis.synchronized {
              val i = currentWorkers.indexWhere(_.atRemovalSafePoint)
              if (i >= 0) {
                Logger.fine(s"Stopping worker $i due to: nWorkers = $nw nUsableWorkers=$nUsableWorkers maxUsableThreads=$maxUsableThreads")
                removeWorker(i)
              }
            }
            // Step the beginTimeOverprovisioned forward by an amount so we don't kill a lot of threads all at once.
            beginTimeOverprovisioned += threadRemoveMinPeriod
          }
        } else {
          // If we are not overprovisioned reset the beginTime for it.
          beginTimeOverprovisioned = currentTime
        }

        if (dumpInterval > 0 && lastDumpTime + dumpInterval <= System.currentTimeMillis()) {
          dumpStats("monitor")
          lastDumpTime = currentTime
        }

        try {
          Thread.sleep(monitorInterval)
        } catch {
          case _: InterruptedException =>
            ()
        }
      }
    }
  }

  final class Worker(var workerID: Int) extends Thread(s"Worker $workerID") {
    private[SimpleWorkStealingScheduler] val workQueue: SchedulingQueue[Schedulable] = new ABPWSDeque(workerQueueLength)

    def queueSize = workQueue.size

    // FIXME: Eliminate these hack fields and figure out a better way to handle this.
    // Fields for use elsewhere. AKA fast thread locals.
    var stackDepth: Int = 0
    var counterOffsets: AnyRef = null

    //@volatile
    var isPotentiallyBlocked = false
    var isInternallyBlocked = true

    @volatile
    private[SimpleWorkStealingScheduler] var isShuttingDown = false

    var newWorks = 0
    var schedules = 0
    var tasksRun = 0
    var steals = 0
    var stealFailures = 0
    var overflows = 0
    var atRemovalSafePoint = false

    private[this] var prngState: Int = workerID + 1 // Must be non-zero
    private[this] var stealFailureRunLength = 0
    private[this] var wasIdle = true

    override def run() = {
      while (!isSchedulerShuttingDown && !isShuttingDown) {
        val workerStart = StopWatches.workerTime.start()
        val t = next()
        if (t != null) {
          isInternallyBlocked = false
          beforeExecute(this, t)
          try {
            SimpleWorkStealingScheduler.enterSchedulable(t, SimpleWorkStealingScheduler.SchedulerExecution)
            if (t.nonblocking) {
              val workStart = StopWatches.workerWorkingTime.start()
              t.run()
              StopWatches.workerWorkingTime.stop(workStart)
            } else {
              // PERFORMANCE: Manually inlined from potentiallyBlocking.
              val workStart = StopWatches.workerWorkingTime.start()
              isPotentiallyBlocked = true
              try {
                t.run()
              } finally {
                isPotentiallyBlocked = false
                StopWatches.workerWorkingTime.stop(workStart)
              }
            }
            SimpleWorkStealingScheduler.exitSchedulable(t)

            afterExecute(this, t, null)
          } catch {
            case ex: Exception =>
              afterExecute(this, t, ex)
          }
          isInternallyBlocked = true
          tasksRun += 1
        }
        StopWatches.workerTime.stop(workerStart)
      }
      onCompleteStealingFailure(this)
    }

    @inline
    def potentiallyBlocking[T](f: => T): T = {
      // This is manually inlined in run above.
      isPotentiallyBlocked = true
      try {
        f
      } finally {
        isPotentiallyBlocked = false
      }
    }

    //@inline
    def scheduleLocal(t: Schedulable): Unit = {
      val r = workQueue.push(t)
      //println(s"$workerID: Scheduled $r")
      if (!r) {
        // The push failed and our queue has overflowed.
        overflows += 1
        // To handle this empty the workQueue into the global workQueue
        var v: Schedulable = workQueue.pop()
        var i = 1
        while (v != null && i < itemsToEvictOnOverflow) {
          inputQueue.add(v)
          v = workQueue.pop()
          i += 1
        }
        if (v != null)
          inputQueue.add(v)

        schedulerThis.synchronized {
          while (i > 0) {
            schedulerThis.notify()
            i -= 1
          }
        }

        scheduleLocal(t)
      } else {
        schedules += 1
      }
    }

    @inline
    private[this] def getLocalRandomNumber() = {
      // An XORShift PRNG
      // This RNG does not pass statistical tests, but that doesn't matter for what we need.
      var x = prngState
      x ^= x << 13
      x ^= x >>> 17
      x ^= x << 5
      prngState = x
      x
    }

    //@inline
    private[this] def next(): Schedulable = {
      val getNewWork = newWorkPeriod > 0 && {
        (getLocalRandomNumber() % newWorkPeriod) == 0
      }

      var t: Schedulable = null

      // To make sure that the inlining works, make sure these functions do not capture any values from their scope.
      @inline
      def ourWork(t: Schedulable): Schedulable = {
        if (t == null) {
          workQueue.pop()
        } else {
          t
        }
      }

      @inline
      def stealWork(t: Schedulable): Schedulable = {
        if (t == null) {
          val u = stealFromAnotherQueue(stealFailureRunLength)
          if (u != null) {
            steals += 1
            stealFailureRunLength = 0
          }
          u
        } else {
          t
        }
      }

      // If we are getting new work, then first try to steal then get from our queue if that doesn't work. Otherwise, try in the other order.
      if (getNewWork) {
        newWorks += 1
        t = stealWork(t)
        t = ourWork(t)
      } else {
        t = ourWork(t)
        t = stealWork(t)
      }

      if (t == null) {
        stealFailures += 1
        stealFailureRunLength += 1
        val n = nWorkers
        val stealAttemptsBeforeBlocking = n * 2
        if (stealFailureRunLength == stealAttemptsBeforeBlocking) {
          // Wipe the queue when we are about to start blocking.
          // This is required to prevent previously scheduled tokens or
          // other schedulables from being kept around by the GC.
          // Overwrite is not needed
          // if the thread is active since it will be constantly
          // overwriting previous tasks as the queue is used.
          workQueue.clean()
        } else if (stealFailureRunLength > stealAttemptsBeforeBlocking) {
          onCompleteStealingFailure(this)
          val waitingStart = StopWatches.workerWaitingTime.start()
          try {
            if (!wasIdle) {
              SimpleWorkStealingScheduler.traceWorkerIdle(this)
              wasIdle = true
            }
            schedulerThis.synchronized {
              atRemovalSafePoint = true
              schedulerThis.wait((stealFailureRunLength - stealAttemptsBeforeBlocking) min maxStealWait)
              atRemovalSafePoint = false
            }
          } catch {
            case _: InterruptedException => ()
          }
          StopWatches.workerWaitingTime.stop(waitingStart)
        }
      }
      if (wasIdle && t != null) {
        SimpleWorkStealingScheduler.traceWorkerBusy(this)
        wasIdle = false
      }
      t
    }

    @inline
    private[this] def stealFromAnotherQueue(i: Int): Schedulable = {
      var t: Schedulable = null
      val index = (getLocalRandomNumber() % nWorkers).abs
      val w = workers(index)
      if (w != null) {
        t = w.workQueue.steal()
        if (t != null)
          SimpleWorkStealingScheduler.traceWorkerSteal(this, w)
      }
      if (t == null) {
        t = inputQueue.poll()
        if (t != null)
          SimpleWorkStealingScheduler.traceWorkerSteal(this, -1)
      }
      t
    }

    def shutdown(): Unit = {
      isShuttingDown = true
    }
  }

  protected[SimpleWorkStealingScheduler] final def addWorker(): Unit = {
    val w = new Worker(nextWorkerID)
    workers(nWorkers) = w
    nWorkers += 1
    nextWorkerID += 1
    w.start()
  }

  protected[SimpleWorkStealingScheduler] final def removeWorker(i: Int): Unit = schedulerThis.synchronized {
    val w = workers(i)
    assert(w.atRemovalSafePoint)
    workers(i) = workers(nWorkers - 1)
    workers(nWorkers - 1) = null
    workers(i).workerID  = i
    nWorkers -= 1
    w.shutdown()
  }

  def dumpStats(name: String) = {
    val ws = currentWorkers

    // Count a thread as working if it is executing a potentially blocking task and is RUNNABLE
    val nBlocked = ws.count(t => {
      t.isPotentiallyBlocked && t.getState != Thread.State.RUNNABLE
    })
    val steals = ws.map(_.steals).sum
    val newWorks = ws.map(_.newWorks).sum
    val stealFailures = ws.map(_.stealFailures).sum
    val overflows = ws.map(_.overflows).sum
    val schedules = ws.map(_.schedules).sum
    val tasksRun = ws.map(_.tasksRun).sum
    val (avgQueueSize, maxQueueSize, minQueueSize) = {
      val queueSizes = ws.map(_.workQueue.size)
      (queueSizes.sum / queueSizes.size, queueSizes.max, queueSizes.min)
    }

    for (w <- ws) {
      w.newWorks = 0
      w.steals = 0
      w.stealFailures = 0
      w.overflows = 0
      w.schedules = 0
      w.tasksRun = 0
    }

    (name, ws.size, nBlocked, steals, stealFailures, newWorks, overflows, schedules, tasksRun, avgQueueSize, maxQueueSize, minQueueSize)
  }

  private val dumpStats = false

  if (dumpStats) {
    DumperRegistry.registerCSVLineDumper(s"work-stealing-scheduler-statistics", "csv", "WorkStealingScheduler statistics",
      Seq(
          "Dump ID [id]",
          "Number of Workers [nWorkers]",
          "Number of Blocked Workers [nBlocked]",
          "Stolen Tasks [steals]",
          "Failed Steal Attempts [stealFailures]",
          "Get New Work from Global Queue [newWorks]",
          "Local Queue Overflows (Overflow triggers some events to be moved to the global queue) [overflows]",
          "Tasks Scheduled [schedules]",
          "Tasks Run [tasksRun]",
          "Average Local Queue Size [avgQueueSize]",
          "Maximum Local Queue Size [maxQueueSize]",
          "Minimum Local Queue Size [minQueueSize]",
      )
    ) { name => dumpStats(name) }
  }

  final def startScheduler(): Unit = synchronized {
    for (_ <- 0 until minWorkers) {
      addWorker()
    }
    monitor.start()
  }

  final def stopScheduler(): Unit = {
    isSchedulerShuttingDown = true
    /*monitor.join()
    for (w <- currentWorkers) {
      w.join()
    }*/
  }

  final def schedule(t: Schedulable): Unit = {
    SimpleWorkStealingScheduler.traceTaskParent(SimpleWorkStealingScheduler.currentSchedulable, t)
    val w = Thread.currentThread()
    if (w.isInstanceOf[Worker]) {
      w.asInstanceOf[Worker].scheduleLocal(t)
    } else {
      inputQueue.add(t)
      schedulerThis.synchronized {
        inputTasks += 1
        schedulerThis.notify()
      }
    }
  }

  final def isOnSchedulerThread = {
    Thread.currentThread().isInstanceOf[Worker]
  }

  def beforeExecute(w: Worker, r: Schedulable): Unit = {}

  def afterExecute(w: Worker, r: Schedulable, t: Throwable): Unit = {}

  /** Called after a worker attempts to steal from all other threads and fails.
    *
    * When this returns the thread will block for a short time to avoid flooding steal
    * operations.
    */
  def onCompleteStealingFailure(w: Worker): Unit = {}
}

object SimpleWorkStealingScheduler {

  final val WorkerIdle = 31L
  orc.util.Tracer.registerEventTypeId(WorkerIdle, "WrkrIdle")

  final val WorkerBusy = 32L
  orc.util.Tracer.registerEventTypeId(WorkerBusy, "WrkrBusy")

  final val WorkerSteal = 36L
  orc.util.Tracer.registerEventTypeId(WorkerSteal, "WrkrStel")

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceScheduler = false

  @inline
  def traceWorkerIdle(workerThread: SimpleWorkStealingScheduler#Worker): Unit = {
    if (traceScheduler) {
      orc.util.Tracer.trace(WorkerIdle, 0L, 0L, 0L)
    }
  }

  @inline
  def traceWorkerBusy(workerThread: SimpleWorkStealingScheduler#Worker): Unit = {
    if (traceScheduler) {
      orc.util.Tracer.trace(WorkerBusy, 0L, 0L, 0L)
    }
  }

  @inline
  def traceWorkerSteal(thiefThread: SimpleWorkStealingScheduler#Worker, victimThread: SimpleWorkStealingScheduler#Worker): Unit = {
    if (traceScheduler) {
      orc.util.Tracer.trace(WorkerSteal, victimThread.getId, thiefThread.workerID, victimThread.workerID)
    }
  }

  @inline
  def traceWorkerSteal(thiefThread: SimpleWorkStealingScheduler#Worker, victim: Long): Unit = {
    if (traceScheduler) {
      orc.util.Tracer.trace(WorkerSteal, victim, thiefThread.workerID, victim)
    }
  }



  final val TaskParent = 33L
  orc.util.Tracer.registerEventTypeId(TaskParent, "TaskPrnt")

  final val TaskStart = 34L
  orc.util.Tracer.registerEventTypeId(TaskStart, "TaskStrt")

  final val TaskEnd = 35L
  orc.util.Tracer.registerEventTypeId(TaskEnd, "TaskEnd ")

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  @inline
  final val traceTasks = false

  if (traceTasks) {
    //DumperRegistry.register(WorkStealingSchedulerTaskTracer.dumpSchedule)
  }

  val nextSchedulableID = new AtomicLong(1)

  val idMap = Collections.synchronizedMap(new WeakHashMap[AnyRef, Long]())

  @inline
  def newSchedulableID() = {
    if (traceTasks) {
      nextSchedulableID.getAndIncrement()
    } else {
      0
    }
  }

  @inline
  def shareSchedulableID(dst: AnyRef, src: AnyRef): Unit = {
    idMap.put(dst, getSchedulableID(src))
  }

  @inline
  def getSchedulableID(s: AnyRef): Long = {
    if (traceTasks) {
      if (s == null) {
        0
      } else {
        s match {
          case l: java.lang.Long => l
          case s =>
            idMap.computeIfAbsent(s, _ => newSchedulableID())
        }
      }
    } else {
      0
    }
  }

  @inline
  def traceTaskParent(parent: AnyRef, child: AnyRef): Unit = {
    traceTaskParent(getSchedulableID(parent), getSchedulableID(child))
  }

  @inline
  def traceTaskParent(parent: AnyRef, child: Long): Unit = {
    traceTaskParent(getSchedulableID(parent), child)
  }

  @inline
  def traceTaskParent(parent: Long, child: Long): Unit = {
    if (traceTasks) {
      orc.util.Tracer.trace(TaskParent, 0L, parent, child)
    }
  }

  val currentSchedulableTL = new ThreadLocal[AnyRef]()

  sealed abstract class SchedulableExecutionType(val id: Long)
  case object SchedulerExecution extends SchedulableExecutionType(0)
  case object StackExecution extends SchedulableExecutionType(1)
  case object InlineExecution extends SchedulableExecutionType(2)

  object SchedulableExecutionType {
    def apply(i: Long): SchedulableExecutionType = {
      i match {
        case 0 => SchedulerExecution
        case 1 => StackExecution
        case 2 => InlineExecution
      }
    }
  }

  val threadMXBean: management.ThreadMXBean = null // java.lang.management.ManagementFactory.getThreadMXBean

  def getCPUTime() = {
    if (threadMXBean != null) {
      threadMXBean.getCurrentThreadCpuTime
    } else 0
  }

  @inline
  def enterSchedulable(s: AnyRef, t: SchedulableExecutionType): Unit = {
    if (traceTasks) {
      orc.util.Tracer.trace(TaskStart, t.id, getSchedulableID(s), getCPUTime())
      currentSchedulableTL.set(s)
    }
  }

  @inline
  def enterSchedulable(s: Long, t: SchedulableExecutionType): Unit = {
    if (traceTasks) {
      orc.util.Tracer.trace(TaskStart, t.id, s, getCPUTime())
      currentSchedulableTL.set(s.asInstanceOf[AnyRef])
    }
  }

  @inline
  def currentSchedulable: AnyRef = {
    if (traceTasks) {
      currentSchedulableTL.get()
    } else {
      null
    }
  }

  @inline
  def exitSchedulable(s: AnyRef): Unit = {
    if (traceTasks) {
      require(s == currentSchedulableTL.get())
      orc.util.Tracer.trace(TaskEnd, 0L, getSchedulableID(s), getCPUTime())
    }
  }

  @inline
  def exitSchedulable(s: AnyRef, old: AnyRef): Unit = {
    if (traceTasks) {
      require(s == currentSchedulableTL.get())
      orc.util.Tracer.trace(TaskEnd, 0L, getSchedulableID(s), 0L)
      currentSchedulableTL.set(old)
    }
  }

  @inline
  def exitSchedulable(s: Long, old: AnyRef): Unit = {
    if (traceTasks) {
      require(s == currentSchedulableTL.get())
      orc.util.Tracer.trace(TaskEnd, 0L, s, 0L)
      currentSchedulableTL.set(old)
    }
  }

}

/** An Orc runtime engine extension which
  * schedules Orc Tokens to run in an OrcThreadPoolExecutor.
  *
  * @author jthywiss, amp
  */
trait OrcWithWorkStealingScheduler extends Orc {
  var scheduler: SimpleWorkStealingScheduler = null

  def startScheduler(options: OrcExecutionOptions): Unit = {
    val maxSiteThreads = if (options.maxSiteThreads > 0) options.maxSiteThreads else 256
    scheduler = new SimpleWorkStealingScheduler(maxSiteThreads) {
      override def beforeExecute(w: Worker, r: Schedulable): Unit = {
        // Allow initialization to be lazily when it is accessed the first time.
      }

      override def afterExecute(w: Worker, r: Schedulable, t: Throwable): Unit = {
        r.onComplete()
        val stage = OrcWithWorkStealingScheduler.stagedTasks.get
        stage.foreach(w.scheduleLocal)
        stage.clear()
        if (t != null) {
          Logger.log(Level.WARNING, s"Schedulable threw exception.", t)
        }
      }
    }

    scheduler.startScheduler()
  }

  def stage(ts: List[Schedulable]): Unit = {
    // We do not check if scheduler is null because it will just throw an NPE and the check might decrease performance on a hot path.
    assert(scheduler.isOnSchedulerThread)
    val stage = OrcWithWorkStealingScheduler.stagedTasks.get
    ts.foreach(_.onSchedule())
    stage ++= ts
  }

  override def stage(t: Schedulable): Unit = {
    // We do not check if scheduler is null because it will just throw an NPE and the check might decrease performance on a hot path.
    assert(scheduler.isOnSchedulerThread)
    val stage = OrcWithWorkStealingScheduler.stagedTasks.get
    t.onSchedule()
    stage += t
  }
  override def stage(a: Schedulable, b: Schedulable): Unit = {
    // We do not check if scheduler is null because it will just throw an NPE and the check might decrease performance on a hot path.
    assert(scheduler.isOnSchedulerThread)
    val stage = OrcWithWorkStealingScheduler.stagedTasks.get
    a.onSchedule()
    b.onSchedule()
    stage += a
    stage += b
  }

  def schedule(t: Schedulable): Unit = {
    // We do not check if scheduler is null because it will just throw an NPE and the check might decrease performance on a hot path.
    t.onSchedule()
    scheduler.schedule(t)
  }

  def stopScheduler(): Unit = {
    if (scheduler != null) {
      scheduler.stopScheduler()
      scheduler = null
    }
  }
}

object OrcWithWorkStealingScheduler {
  val stagedTasks = new ThreadLocal[ArrayBuffer[Schedulable]]() {
    override def initialValue() = new ArrayBuffer[Schedulable]()
  }
}
