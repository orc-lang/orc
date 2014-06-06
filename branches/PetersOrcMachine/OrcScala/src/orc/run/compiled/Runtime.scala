//
// Runtime.scala -- Scala class/trait/object Runtime
// Project OrcScala
//
// $Id$
//
// Created by amp on May 21, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.compiled

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import orc.OrcEvent
import orc.OrcExecutionOptions
import java.util.Timer
import java.util.TimerTask

class ExecutionHandle(initialElements: Counter*) {
  import scala.collection.JavaConversions._

  // FIXME: This holds references to counters for an unbounded amount of time. It might be better to allow them to remove themselves when they zero. This could even be converted to a counter in that case. 
  val counters = new ConcurrentLinkedQueue[Counter]
  counters.addAll(initialElements)

  def waitForHalt() {
    while (!counters.isEmpty()) {
      val c = counters.poll()
      Logger.fine(s"Waiting on counter: $c")
      c.waitZero()
      Logger.fine(s"Counter zeroed: $c")
    }
  }

  def addCounter(c: Counter) {
    Logger.fine(s"Adding counter: $c")
    counters.offer(c)
  }
}

case class ResilientCounterEvent(c: Counter) extends OrcEvent

/**
  * @author amp
  */
class Runtime {

  private val runningCount: AtomicInteger = new AtomicInteger(1)

  private val processors = Runtime.getRuntime().availableProcessors()
  
  // FIXME: Blocked threads are not replaced. I need a facility that marks blocked threads and spawns new ones. 
  // spawn 4 threads per core (currently do not worry about blocked threads)
  val nthreads = if (System.getProperty("orc.nprocessors") ne null) System.getProperty("orc.nprocessors").toInt else processors * 4

  private val threads: IndexedSeq[RuntimeThread] = for (_ <- 1 to nthreads) yield {
    new RuntimeThread(this)
  }
  runningCount.set(threads.size)

  // Tell them all about the other contexts and start them all.
  for (t <- threads) {
    t.otherContexts = threads.filterNot(_ ne t)
    t.start()
  }

  Logger.logAllToStderr()

  def start(e: OrcModule, k: OrcEvent => Unit, options: OrcExecutionOptions): ExecutionHandle = {
    // Insert the initial program state into one of the threads.
    val initCounter = new Counter()

    val executionHandle = new ExecutionHandle(initCounter)
    def eventHandler(e: OrcEvent) = e match {
      case ResilientCounterEvent(c) =>
        executionHandle.addCounter(c)
      case _ =>
        k(e)
    }

    val instance = e(initCounter, eventHandler, options, threads(0))
    
    threads(0).schedule(() => {
      instance()
    })

    executionHandle
  }

  def kill() {
    for (t <- threads) {
      t.kill()
    }

    timer.cancel()
  }

  def waitForWork() {
    if (runningCount.decrementAndGet() == 0) {
      // We are done, All threads are waiting for work.
      //kill()
    } else {
      // TODO: This should be some kinda of smart blocking not polling. At least there should be back off.
      Thread.sleep(0, 100)
      runningCount.incrementAndGet()
    }
  }

  val timer: Timer = new Timer()

  def registerDelayed(delay: Long, f: () => Unit) = {
    val callback =
      new TimerTask() {
        @Override
        override def run() { f() }
      }
    timer.schedule(callback, delay)
  }
}
