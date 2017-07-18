//
// WorkStealingScheduler.scala -- A work-stealing scheduler for Orc
// Project OrcScala
//
// Created by amp on Feb, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.mutable.ArrayBuffer

import orc.{ OrcExecutionOptions, Schedulable }
import orc.run.Orc
import orc.util.ABPWSDeque
import java.util.logging.Level

/**
 *
 * @author amp
 */
class SimpleWorkStealingScheduler(
  maxSiteThreads: Int,
  val monitorInterval: Int = 10,
  val goalExtraThreads: Int = 0,
  val workerQueueLength: Int = 256) {
  schedulerThis =>
  val nCores = Runtime.getRuntime().availableProcessors()
  val minWorkers = math.max(4, nCores * 2)
  val maxWorkers = minWorkers + maxSiteThreads
  val maxStealWait = 20
  val goalUsableThreads = minWorkers + goalExtraThreads
  val maxUsableThreads = goalUsableThreads * 2
  val dumpInterval = -1 // 1000
  val itemsToEvictOnOverflow = workerQueueLength / 4

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
      while (!isSchedulerShuttingDown) {
        val ws = currentWorkers
        val nw = ws.size

        // Count a thread as working if it is executing a potentially bloacking task and is RUNNABLE
        val nBlocked = ws.count(t => {
          t.isPotentiallyBlocked && t.getState != Thread.State.RUNNABLE
        })

        val nUsableWorkers = nw - nBlocked

        if (nUsableWorkers < goalUsableThreads && nWorkers < maxWorkers) {
          Logger.fine(s"Starting new worker due to: nWorkers = $nw nUsableWorkers=$nUsableWorkers goalUsableThreads=$goalUsableThreads nBlocked=$nBlocked")
          addWorker()
        }
        if (nUsableWorkers > maxUsableThreads && nWorkers > minWorkers) {
          Logger.fine(s"Stopping worker due to: nWorkers = $nw nUsableWorkers=$nUsableWorkers maxUsableThreads=$maxUsableThreads")
          schedulerThis.synchronized {
            val i = currentWorkers.indexWhere(_.atRemovalSafePoint)
            if(i >= 0)
              removeWorker(i)
          }
        }

        if (dumpInterval > 0 && lastDumpTime + dumpInterval <= System.currentTimeMillis()) {
          dumpStats()
          lastDumpTime = System.currentTimeMillis()
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

  @sun.misc.Contended
  final class Worker(workerID: Int) extends Thread(s"Worker $workerID") {
    private[SimpleWorkStealingScheduler] val workQueue = new ABPWSDeque[Schedulable](workerQueueLength)

    @volatile
    private[SimpleWorkStealingScheduler] var isPotentiallyBlocked = false

    @volatile
    private[SimpleWorkStealingScheduler] var isShuttingDown = false

    var steals = 0
    var stealFailures = 0
    var overflows = 0
    var atRemovalSafePoint = false

    private[this] var seed: Int = workerID * 997
    private[this] var stealFailureRunLength = 0

    override def run() = {
      while (!isSchedulerShuttingDown && !isShuttingDown) {
        val t = next()
        if (t != null) {
          orc.util.Profiler.measureInterval(0L, 'SimpleWorkStealingScheduler_beforeExecute) {
            beforeExecute(this, t)
          }
          try {
            val tClassName = t.getClass.getSimpleName
            
            // The symbol is a too expensive for the hot path: orc.util.Profiler.measureInterval(0L, Symbol(tClassName+"_run *")) 
            {
              if (t.nonblocking) {
                t.run()
              } else {
                potentiallyBlocking(t.run())
              }
            }
            
            // The symbol is a too expensive for the hot path: orc.util.Profiler.measureInterval(0L, 'SimpleWorkStealingScheduler_afterExecute) 
            {
              afterExecute(this, t, null)
            }
          } catch {
            case ex: Exception =>
              // The symbol is a too expensive for the hot path: orc.util.Profiler.measureInterval(0L, 'SimpleWorkStealingScheduler_afterExecute) 
              {
                afterExecute(this, t, ex)
              }
          }
        }
      }
    }

    @inline
    def potentiallyBlocking[T](f: => T): T = {
      isPotentiallyBlocked = true
      try {
        f
      } finally {
        isPotentiallyBlocked = false
      }
    }

    //@inline
    def scheduleLocal(t: Schedulable): Unit = {
      val r = workQueue.pushBottom(t)
      //println(s"$workerID: Scheduled $r")
      if (!r) {
        // The push failed and our queue has overflowed.
        overflows += 1
        // To handle this empty the workQueue into the global workQueue
        var v: Schedulable = workQueue.popBottom()
        var i = 1
        while (v != null && i < itemsToEvictOnOverflow) {
          inputQueue.add(v)
          v = workQueue.popBottom()
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
      }
    }

    //@inline
    private[this] def next(): Schedulable = {
      var t = workQueue.popBottom()
      if (t == null) {
        t = stealFromAnotherQueue(stealFailureRunLength)
        if (t != null) {
          steals += 1
          stealFailureRunLength = 0
          seed = seed * 997 + 449 + workerID
        }
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
          workQueue.wipe()
        } else if (stealFailureRunLength > stealAttemptsBeforeBlocking) {
          try {
            schedulerThis.synchronized {
              atRemovalSafePoint = true
              schedulerThis.wait((stealFailureRunLength - n * 2) min maxStealWait)
              atRemovalSafePoint = false
            }
          } catch {
            case _: InterruptedException => ()
          }
        }
      }
      t
    }

    @inline
    private[this] def stealFromAnotherQueue(i: Int): Schedulable = {
      var t = inputQueue.poll()
      if (t == null) {
        val index = ((i + workerID * 997 + seed) % nWorkers).abs
        val w = workers(index)
        if (w != null) {
          t = w.workQueue.popTop()
        }
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
    nWorkers -= 1
    w.shutdown()
  }

  def dumpStats(): Unit = {
    val buf = new StringBuilder()
    def statLine(s: String) = {
      buf ++= s
      buf += '\n'
    }
    val ws = currentWorkers
    statLine(s"workers.size = ${workers.size}")
    statLine(s"nWorkers = ${ws.size}")
    val steals = ws.map(_.steals).sum
    statLine(s"steals = ${steals}")
    val stealFailures = ws.map(_.stealFailures).sum
    statLine(s"stealFailures = ${stealFailures}")
    val overflows = ws.map(_.overflows).sum
    statLine(s"overflows = ${overflows}")
    val (avgQueueSize, maxQueueSize, minQueueSize) = {
      val queueSizes = ws.map(_.workQueue.size())
      (queueSizes.sum / queueSizes.size, queueSizes.max, queueSizes.min)
    }
    statLine(s"(avgQueueSize, maxQueueSize, minQueueSize) = ${(avgQueueSize, maxQueueSize, minQueueSize)}")
    val inputQueueSize = inputQueue.size()
    statLine(s"inputQueueSize = ${inputQueueSize}")

    statLine(s"inputTasks = ${inputTasks}")

    Logger.info(buf.toString())
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
}

/** An Orc runtime engine extension which
  * schedules Orc Tokens to run in an OrcThreadPoolExecutor.
  *
  * @author jthywiss
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
