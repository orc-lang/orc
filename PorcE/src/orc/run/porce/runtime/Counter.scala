//
// Counter.scala -- Scala class Counter
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.io.StringWriter
import java.io.PrintWriter
import java.util.concurrent.LinkedBlockingDeque
import scala.annotation.elidable
import orc.run.porce.Logger
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

object Counter {
  val liveCounters = new LinkedBlockingDeque[Counter]()

  def exceptionString(e: Exception) = {
    val ss = new StringWriter()
    e.printStackTrace(new PrintWriter(ss))
    ss.toString()
  }

  @TruffleBoundary
  def report() = {
    if (Logger.julLogger.isLoggable(Level.FINE)) {
      import scala.collection.JavaConverters._
      Logger.fine(s"Live Counter Report: ${liveCounters.size}")
      for (c <- liveCounters.asScala) {
        Logger.fine(s"$c: log size = ${c.log.size}, count = ${c.count.get}")
      }
      for (c <- liveCounters.asScala) {
        Logger.fine(s"$c:\n${c.log.asScala.map(exceptionString(_)).mkString("----\n")}")
      }
    } else {
      Logger.warning(s"Cannot report Counter information if FINE is not loggable in ${Logger.julLogger.getName}")
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary
  def addCounter(c: Counter) = {
    if (Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.add(c)
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary
  def removeCounter(c: Counter) = {
    if (Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.remove(c)
    }
  }
}

/** @author amp
  */
abstract class Counter {
  /*
  @elidable(elidable.ASSERTION)
  val log = new LinkedBlockingDeque[Exception]()

  @elidable(elidable.ASSERTION)
  @inline
  private def logChange(s: => String) = {
    if (Logger.julLogger.isLoggable(Level.FINE)) {
      log.add(new Exception(s))
    }
  }
  logChange(s"Init to 1")

  Counter.addCounter(this)
  // */

  val log: LinkedBlockingDeque[Exception] = null
  @inline
  private def logChange(s: => String) = {
  }
  // */

  /** The number of executions that are either running or pending.
    *
    * This functions similarly to a reference count and this halts when count
    * reaches 0.
    */
  val count = new AtomicInteger(1)
  @volatile
  var isDiscorporated = false

  def setDiscorporate() = {
    assert(count.get() > 0)
    isDiscorporated = true
  }

  def discorporate() = {
    setDiscorporate()
    halt()
  }

  /** Decrement the count and check for overall halting.
    *
    * If we did halt call onContextHalted().
    */
  def halt(): Unit = {
    val n = count.decrementAndGet()
    /*
    logChange(s"- Down to $n")
    if (n < 0) {
      Counter.report()
    }
    assert(n >= 0, s"Halt is not allowed on already stopped Counters: $this")
    */
    if (n == 0) {
      // Counter.removeCounter(this)
      onContextHalted()
    }
  }

  /** Increment the count.
    */
  def prepareSpawn(): Unit = {
    val n = count.getAndIncrement()
    /*
    logChange(s"+ Up from $n")
    if (n <= 0) {
      Counter.report()
    }
    assert(n > 0, s"Spawning is not allowed once we go to zero count. No zombies allowed!!! $this")
    */
  }

  /** Called when this whole context has halted.
    */
  def onContextHalted(): Unit
}

/** @author amp
  */
abstract class CounterNestedBase(parent: Counter) extends Counter {
  // Matched against: onContextHalted call to halt
  parent.prepareSpawn()

  /** Called when this whole context has halted.
    */
  def onContextHalted(): Unit = {
    // Matched against: constructor call to prepareSpawn
    if (isDiscorporated)
      parent.discorporate()
    else
      parent.halt()
  }
}

/** @author amp
  */
final class CounterNested(execution: PorcEExecution, parent: Counter, haltContinuation: PorcEClosure) extends CounterNestedBase(parent) {
  /** Called when this whole context has halted.
    */
  override def onContextHalted(): Unit = {
    if (!isDiscorporated) {
      execution.runtime.scheduleOrCall(parent, () => haltContinuation.callFromRuntime())
    }
    super.onContextHalted()
  }
}
