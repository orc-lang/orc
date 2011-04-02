//
// ThreadPoolScheduler.scala -- Scala traits OrcWithThreadPoolScheduler, OrcRunner, and OrcThreadPoolExecutor
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Mar 29, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Level

import orc.Handle
import orc.OrcExecutionOptions
import orc.run.Orc
import orc.run.Logger

/**
 * An Orc runtime engine extension which
 * schedules Orc Tokens to run in an OrcThreadPoolExecutor.
 *
 * @author jthywiss
 */
trait OrcWithThreadPoolScheduler extends Orc {
  
  private var executor: OrcRunner = null

  /* (non-Javadoc)
   * @see orc.OrcRuntime#schedule(scala.collection.immutable.List)
   */
  override def schedule(ts: List[Token]) {
    ts.foreach(schedule(_))
  }

  /* (non-Javadoc)
   * @see orc.OrcRuntime#schedule(orc.OrcRuntime.Token, orc.OrcRuntime.Token)
   */
  override def schedule(t: Token, u: Token) {
    schedule(t)
    schedule(u)
  }

  /* (non-Javadoc)
   * @see orc.OrcRuntime#schedule(orc.OrcRuntime.Token)
   */
  override def schedule(t: Token) {
    executor.execute(t, false)
  }
  
  override def schedule(h: Handle) {
    executor.execute(h, true)
  }

  override def startScheduler(options: OrcExecutionOptions) {
    if (executor == null) {
      executor = new OrcThreadPoolExecutor(options.maxSiteThreads)
      executor.startup()
    } else {
      throw new IllegalStateException("startScheduler() mutiply invoked")
    }
  }
  
  /* (non-Javadoc)
   * @see orc.run.Orc#stop()
   */
  override def stopScheduler() {
    if (executor != null) {
      executor.shutdownNow()
      executor = null
    } else {
      throw new IllegalStateException("stopScheduler() mutiply invoked")
    }
  }
}


/**
 * Interface from Orc runtime engine to an executor service
 *
 * @author jthywiss
 */
trait OrcRunner {

  type Task = Runnable

  /** Begin executing submitted tasks */
  def startup(): Unit

  /** Submit task for execution */
  def execute(task: Task, taskMayBlock: Boolean): Unit

  /** Orderly shutdown; let running tasks complete */
  def shutdown(): Unit

  /** Attempt immediate shutdown; interrupt running tasks
   * @return List of queued tasks discarded
   */
  def shutdownNow(): java.util.List[Task]

}


/**
 * A ThreadPoolExecutor that periodically resizes the work thread pool
 * to ensure there is a minimum number of runnable threads.  I.e., as
 * threads are blocked by their task, new threads are added to serve
 * the work queue.
 *
 * @author jthywiss
 */
class OrcThreadPoolExecutor(maxSiteThreads: Int) extends ThreadPoolExecutor(
    //TODO: Make more of these params configurable
    math.max(4, Runtime.getRuntime().availableProcessors * 2),
    if (maxSiteThreads > 0) maxSiteThreads else 256,
    60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable],
    new ThreadPoolExecutor.CallerRunsPolicy) with OrcRunner with Runnable {

  val threadGroup = new ThreadGroup("Orc Runtime Engine ThreadGroup")

  object OrcWorkerThreadFactory extends ThreadFactory {
    var threadCreateCount = 0
    protected def getNewThreadName() = {
      var ourThreadNum = 0
      synchronized {
        ourThreadNum = threadCreateCount
        threadCreateCount += 1
      }
      "Orc Worker Thread " + ourThreadNum
    }
    def newThread(r: Runnable): Thread = {
      new Thread(threadGroup, r, getNewThreadName())
    }
  }
  
  setThreadFactory(OrcWorkerThreadFactory)

  @scala.volatile private var supervisorThread: Thread = null
  def startup() {
    synchronized {
      if (supervisorThread != null) {
        throw new IllegalStateException("OrcThreadPoolExecutor.startup() on a started instance")
      }
      supervisorThread = new Thread(threadGroup, this, "Orc Runtime Engine Thread Pool Supervisor")
      supervisorThread.start()
    }
  }

  override def execute(task: Task, taskMayBlock: Boolean): Unit = {
    if (supervisorThread == null) {
      throw new IllegalStateException("OrcThreadPoolExecutor.execute() on an un-started instance")
    }
    //FIXME: Don't allow blocking tasks to consume all worker threads
    super.execute(task)
  }

  protected val CHECK_PERIOD = 10 /* milliseconds */

  override def run() {
    val numCores = Runtime.getRuntime().availableProcessors()
    val mainLockField = getClass.getSuperclass.getDeclaredField("mainLock")
    mainLockField.setAccessible(true)
    val mainLock = mainLockField.get(this).asInstanceOf[java.util.concurrent.locks.ReentrantLock]

    try {
      while (!isShutdown) {
        try {
          Thread.sleep(CHECK_PERIOD)
        } catch {
          case _: InterruptedException => Thread.interrupted // Reset interrupted state
        }

        try {
          mainLock.lock()
          
          val threadBuffer = new Array[Thread](threadGroup.activeCount)
          val liveThreads = threadBuffer.take(threadGroup.enumerate(threadBuffer, false))
          val supervisor = this
          val totalThreadCount = liveThreads.count({t => t != supervisor})
          val runnableThreadCount = liveThreads.count({t => t != supervisor && t.getState == Thread.State.RUNNABLE})
          val nonRunnableThreadCount = totalThreadCount - runnableThreadCount
          val nonBlockedThreadCount = liveThreads.count({t => t != supervisor && t.getState != Thread.State.BLOCKED})

          // Thread pool size needs to be adjusted for workers that are consumed by blocking
          // We want RUNNABLE threads == # CPU cores * 2
          setCorePoolSize(math.min(math.max(4, numCores + nonRunnableThreadCount), getMaximumPoolSize))

          if (getQueue.isEmpty && nonBlockedThreadCount == 0) {
            Logger.finest(getClass.getCanonicalName+".run(): No more work, shutting down")
            shutdown()
          }
        } finally {
          mainLock.unlock()
        }
      }
    } catch {
      case t => { t.printStackTrace(); Logger.log(Level.SEVERE, "Exception in "+getClass.getCanonicalName+".run()", t); shutdownNow(); throw t }
    } finally {
      logThreadExit()
    }
    Logger.exiting(getClass.getCanonicalName, "run")
  }

  Logger.finer(getClass.getCanonicalName+": Constructed")
  Logger.finest("corePoolSize = " + getCorePoolSize)
  Logger.finest("maximumPoolSize = " + getMaximumPoolSize)

  def logThreadExit() = {
    Logger.finer(getClass.getCanonicalName+": Supervisor thread exit")
    Logger.finest("corePoolSize = " + getCorePoolSize)
    Logger.finest("maximumPoolSize = " + getMaximumPoolSize)
    Logger.finest("poolSize = " + getPoolSize)
    Logger.finest("activeCount = " + getActiveCount)
    Logger.finest("largestPoolSize = " + getLargestPoolSize)
    Logger.finest("taskCount = " + getTaskCount)
    Logger.finest("completedTaskCount = " + getCompletedTaskCount)
    Logger.finest("Worker threads creation count: " + OrcWorkerThreadFactory.threadCreateCount)
  }

}
