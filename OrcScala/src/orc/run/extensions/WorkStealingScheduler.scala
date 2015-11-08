//
// ThreadPoolScheduler.scala -- Scala traits OrcWithThreadPoolScheduler, OrcRunner, and OrcThreadPoolExecutor
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Mar 29, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import java.util.concurrent.{ LinkedBlockingQueue, ThreadFactory, ForkJoinPool, ForkJoinTask, TimeUnit }
import java.util.logging.Level
import orc.{ OrcExecutionOptions, Schedulable }
import orc.run.Orc
import java.util.concurrent.ForkJoinWorkerThread
import scala.collection.mutable

/** An Orc runtime engine extension which
  * schedules Orc Tokens to run in an OrcThreadPoolExecutor.
  *
  * @author jthywiss
  */
trait OrcWithWorkStealingScheduler extends OrcWithThreadPoolScheduler {
  override def startScheduler(options: OrcExecutionOptions) {
    Logger.entering(getClass().getCanonicalName(), "startScheduler")
    executorLock synchronized {
      if (executor == null) {
        executor = new OrcWorkStealingExecutor(engineInstanceName, options.maxSiteThreads)
        executor.startupRunner()
      } else {
        throw new IllegalStateException("startScheduler() multiply invoked")
      }
    }
  }
}

object OrcWorkStealingExecutor {
  final class OrcWorkerThread(p: ForkJoinPool, name: String) extends ForkJoinWorkerThread(p) {
    val stagedTasks = new mutable.ArrayBuffer[Schedulable](4)
    setName(name)
  }
  final class OrcWorkerThreadFactory(engineInstanceName: String) extends ForkJoinPool.ForkJoinWorkerThreadFactory {
    val threadGroup = new ThreadGroup(engineInstanceName + " ThreadGroup")
    var threadCreateCount = 0
    protected def getNewThreadName() = {
      var ourThreadNum = 0
      synchronized {
        ourThreadNum = threadCreateCount
        threadCreateCount += 1
      }
      engineInstanceName + " Worker Thread " + ourThreadNum
    }
    def newThread(p: ForkJoinPool): ForkJoinWorkerThread = {
      new OrcWorkerThread(p, getNewThreadName())
    }
  }
  
  @inline
  def currentOrcWorkerThread() = {
    Thread.currentThread() match {
      case t : OrcWorkerThread =>
        t
      case t =>
        throw new AssertionError(s"currentOrcWorkerThread called from a non scheduler worker thread: $t (${t.getClass})")
    }
  }
  
  final class OrcForkJoinTask(task: Schedulable) extends ForkJoinTask[Void] {
    final def getRawResult() : Void = null
    final def setRawResult(v : Void) = ()
    final def exec() : Boolean = {
      try {
        if (task.nonblocking)
          task.run()
        else
          ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker {
            var done = false
            def block() = {
              task.run()
              done = true
              true
            }
            def isReleasable() = done
          })
      } catch {
        case e: Throwable => 
          Logger.log(Level.SEVERE, "Exception in schedulable run", e)
      } finally {
        task.onComplete()
        val t = currentOrcWorkerThread()
        t.stagedTasks.foreach(s => new OrcForkJoinTask(s).fork())
        t.stagedTasks.clear()
      }
      true
    }
  }

}

/** A ThreadPoolExecutor that periodically resizes the worker thread pool
  * to ensure there is a minimum number of runnable threads.  I.e., as
  * threads are blocked by their task, new threads are added to serve
  * the work queue.
  *
  * @author jthywiss
  */
final class OrcWorkStealingExecutor(engineInstanceName: String, maxSiteThreads: Int) extends ForkJoinPool(
  //TODO: Make more of these params configurable
  math.max(4, Runtime.getRuntime().availableProcessors * 2),
  // We want our own threads
  new OrcWorkStealingExecutor.OrcWorkerThreadFactory(engineInstanceName),
  // Default exception handler
  null,
  // Use async mode (FIFO queue instead of LIFO stack of tasks)
  true
  ) with OrcRunner with Runnable {
  import OrcWorkStealingExecutor._

  val threadGroup = getFactory().asInstanceOf[OrcWorkStealingExecutor.OrcWorkerThreadFactory].threadGroup
  val maximumPoolSize = if (maxSiteThreads > 0) (math.max(4, Runtime.getRuntime().availableProcessors * 2) + maxSiteThreads) else 256
  
  private var supervisorThread: Thread = null
  @scala.volatile private var onShutdownStart: () => Unit = { () => }
  @scala.volatile private var onShutdownFinish: () => Unit = { () => }

  @throws(classOf[IllegalStateException])
  @throws(classOf[SecurityException])
  def startupRunner() {
    synchronized {
      if (supervisorThread != null || isShutdown) {
        throw new IllegalStateException("OrcThreadPoolExecutor.startup() on a started instance")
      }
      supervisorThread = new Thread(threadGroup, this, engineInstanceName + " Thread Pool Supervisor")
      supervisorThread.start()
    }
  }

  @throws(classOf[IllegalStateException])
  def stageTask(task: Schedulable) {
    if (supervisorThread == null) {
      throw new IllegalStateException("OrcThreadPoolExecutor.execute() on an un-started instance")
    }
    currentOrcWorkerThread().stagedTasks += task
  }
  
  @throws(classOf[IllegalStateException])
  @throws(classOf[SecurityException])
  def executeTask(task: Schedulable) {
    if (supervisorThread == null) {
      throw new IllegalStateException("OrcThreadPoolExecutor.execute() on an un-started instance")
    }
    //FIXME: Don't allow blocking tasks to consume all worker threads
    val fjtask = new OrcForkJoinTask(task)
    // Check if we are already in the thread pool
    if(Thread.currentThread().isInstanceOf[OrcWorkerThread])
      fjtask.fork()
    else
      execute(fjtask)
    // Use task.fork() here instead of this.execute because this.execute will not insert the task into the current threads queue. 
  }

  @throws(classOf[IllegalStateException])
  @throws(classOf[SecurityException])
  def shutdownRunner(onShutdownStart: () => Unit, onShutdownFinish: () => Unit) {
    val t = supervisorThread
    if (t != null) {
      this.onShutdownStart = onShutdownStart
      this.onShutdownFinish = onShutdownFinish
      t.interrupt()
    } else {
      onShutdownStart()
      onShutdownFinish()
    }
  }

  def awaitTermination(timeoutMillis: Long) = {
    super.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)
  }

  // TODO: Make sure this is called at the right time.
  protected def terminated() {
    val t = supervisorThread
    if (t != null) {
      t.interrupt()
    }
  }

  protected val CHECK_PERIOD = 10 /* milliseconds */

  override def run() {
    // For now do no supervision.
    // TODO: Integrate with ForkJoinPool blocking handling.
    /*
    var shutdownRequested = false
    var giveUp = false
    val numCores = Runtime.getRuntime().availableProcessors()
    val mainLockField = getClass.getSuperclass.getDeclaredField("mainLock")
    mainLockField.setAccessible(true)
    val mainLock = mainLockField.get(this).asInstanceOf[java.util.concurrent.locks.ReentrantLock]
    val threadBuffer = new Array[Thread](maximumPoolSize + 2)
    var firstTime = 0L
    var lastTime = Long.MinValue

    try {
      while (!isTerminated && !giveUp) {
        try {
          if (shutdownRequested) {
            if (firstTime == 0) firstTime = System.currentTimeMillis()
            val currTime = System.currentTimeMillis() - firstTime

            def ifElapsed(triggerTime: Long, action: => Unit) = {
              if (currTime >= triggerTime && triggerTime > lastTime) {
                Logger.finest("At shutdown elapsed time " + currTime + " ms, firing action scheduled for " + triggerTime + " ms")
                action
              }
            }

            // First, gently shut down
            ifElapsed(0L, { onShutdownStart(); shutdown() })
            // After "a little while", we insist
            ifElapsed(120L, { shutdownNow() })
            // Wait 5.05 min for all running workers to shutdown (5 min for TCP timeout)
            ifElapsed(303000L, {
              Logger.severe(s"Orc shutdown was unable to terminate ${getPoolSize()} worker threads, after trying for ~5 minutes\n" +
                threadGroupDump(threadGroup))
              giveUp = true
            })

            lastTime = currTime
          }

          if (!isTerminated && !giveUp) {
            try {
              mainLock.lock()

              // Java thread states are:
              // NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED
              // RUNNABLE means can be or is running on a core
              // BLOCKED means waiting on a monitor (synchronized), so that's like RUNNABLE for us
              // WAITING, TIMED_WAITING, TERMINATED may never come back to make progress
              // However, some WAITING/TIMED_WAITING threads are actually waiting for new tasks
              // We want enough RUNNABLE+BLOCKED threads to keep all CPU cores busy, but not more.

              // This approach is stochastic; and the following calculation is approximate -- there are transients
              val liveThreads = threadBuffer.view(0, threadGroup.enumerate(threadBuffer, false))
              val workingThreads = getActiveCount // Number of Workers running a Task
              val supervisor = Thread.currentThread
              val progressingThreadCount = liveThreads.count({ t => t != supervisor && (t.getState == Thread.State.RUNNABLE || t.getState == Thread.State.BLOCKED || t.getState == Thread.State.NEW) })
              val nonProgressingWorkingThreadCount = workingThreads - progressingThreadCount

              //Logger.finest("poolSize = " + getPoolSize)
              //Logger.finest("workingThreads = " + workingThreads)
              //Logger.finest(liveThreads.filter({t => t != supervisor}).map(_.getState.toString + "  ").foldLeft("Thread States:  ")({(x,y)=>x+y}))
              //Logger.finest("progressingThreadCount = " + progressingThreadCount)
              //Logger.finest("nonProgressingWorkingThreadCount = " + nonProgressingWorkingThreadCount)
              //Logger.finest("numCores*2 + nonProgressingTaskCount = " + (numCores*2 + nonProgressingWorkingThreadCount))

              for (i <- 0 until threadBuffer.length) {
                threadBuffer.update(i, null)
              }

              setCorePoolSize(math.min(math.max(4, numCores * 2 + nonProgressingWorkingThreadCount), getMaximumPoolSize))
            } finally {
              mainLock.unlock()
            }

            Thread.sleep(CHECK_PERIOD)

          }
        } catch {
          case _: InterruptedException => {
            Logger.finest("Supervisor interrupt -- shutdown starting...")
            Thread.interrupted // Reset interrupted state
            shutdownRequested = true // If someone interrupted, we should shutdown
          }
        }
      }
    } catch {
      case t: Throwable => { t.printStackTrace(); Logger.log(Level.SEVERE, "Caught in " + getClass.getCanonicalName + ".run()", t); shutdownNow(); throw t }
    } finally {
      try {
        if (!isTerminated) shutdownNow()
        onShutdownFinish()
      } catch {
        case _: Throwable => /* Do nothing */
      }
      supervisorThread.getThreadGroup().setDaemon(true)
      logThreadExit()
      Logger.finest("Executor shutdown time: " + (System.currentTimeMillis() - firstTime) + " ms")
      supervisorThread = null;
    }
    Logger.exiting(getClass.getCanonicalName, "run")
    */
  }

  Logger.finer(getClass.getCanonicalName + ": Constructed")

  def logThreadExit() = {
    Logger.finer(getClass.getCanonicalName + ": Supervisor thread exit")
    Logger.finest("poolSize = " + getPoolSize)
  }

  def threadGroupDump(threadGroup: ThreadGroup): String = {
    val threadBuffer = new Array[Thread](threadGroup.activeCount + 5)
    val descendantThreads = threadBuffer.view(0, threadGroup.enumerate(threadBuffer, true))
    val threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean
    val deadlockIds = threadMXBean.findDeadlockedThreads()
    (if (deadlockIds != null && deadlockIds.length > 0)
      "DEADLOCK DETECTED among these threads in the JVM:\n" +
      (for (tid <- deadlockIds) yield {
        val ti = threadMXBean.getThreadInfo(tid)
        s"\t'${ti.getThreadName()}' tid=${tid} ${ti.getThreadState}" +
          (if (ti.getLockName != null) " on " + ti.getLockName else "") +
          (if (ti.getLockOwnerName != null) s" owned by '${ti.getLockOwnerName}' tid=${ti.getLockOwnerId()}" else "") +
          '\n'
      }).mkString + "\n"
    else
      "") +
      threadGroup.getName + " thread dump:\n\n" +
      (for (thread <- descendantThreads) yield {
        val stackTrace = thread.getStackTrace
        val ti = threadMXBean.getThreadInfo(thread.getId)
        s"'${thread.getName}' ${if (thread.isDaemon) "daemon " else ""}prio=${thread.getPriority} tid=${thread.getId} ${thread.getState}" +
          (if (ti.getLockName != null) " on " + ti.getLockName else "") +
          (if (ti.getLockOwnerName != null) s" owned by '${ti.getLockOwnerName}' tid=${ti.getLockOwnerId()}" else "") +
          (if (ti.isSuspended) " (suspended)" else "") +
          (if (ti.isInNative) " (in native)" else "") +
          "\n" +
          (for (i <- 0 until stackTrace.length)
            yield s"\tat ${stackTrace(i)}\n" +
            (for {
              mi <- ti.getLockedMonitors
              if mi.getLockedStackDepth() == i
            } yield {
              s"\t-  locked  $mi\n"
            }).mkString).mkString + "\n"
      }).mkString
  }

}
