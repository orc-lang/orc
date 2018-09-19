//
// VirtualClock.scala -- Scala class VirtualClock, trait VirtualClockOperation, and class AwaitCallController
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

import scala.collection.mutable

import orc.{ OrcRuntime, Schedulable }
import orc.error.runtime.VirtualClockError
import orc.values.sites.{ Site, SpecificArity }

/** @author dkitchin
  */
trait VirtualClockOperation extends Site with SpecificArity

/** @author dkitchin
  */
class AwaitCallController(caller: Token) extends CallController(caller) {
  override def toString() = super.toString + s"(caller=$caller)"
  /** An expensive walk-to-root check for alive state */
  def checkAlive(): Boolean = isLive && caller.checkAlive()
}

/** @author dkitchin
  */
class VirtualClock(ordering: (AnyRef, AnyRef) => Int, runtime: OrcRuntime)
  extends Schedulable {

  override val nonblocking = true

  type Time = AnyRef

  val queueOrder = new Ordering[(AwaitCallController, Time)] {
    def compare(x: (AwaitCallController, Time), y: (AwaitCallController, Time)) = ordering(y._2, x._2)
  }

  var currentTime: Option[Time] = None
  val waiterQueue: mutable.PriorityQueue[(AwaitCallController, Time)] = new mutable.PriorityQueue()(queueOrder)

  private var readyCount: Int = 0

  /** Take all minimum time elements from the waiter queue.
    * Return Some tuple containing the minimum time and a nonempty list of waiters on that time.
    * If the queue is empty, return None instead.
    */
  private def dequeueMins(): Option[(Time, List[AwaitCallController])] = {
    /* Don't advance clock to time of a killed Vawait call.
     * Possible race with kill(), best effort only. */
    while (waiterQueue.headOption.exists(!_._1.checkAlive())) waiterQueue.dequeue()

    waiterQueue.headOption match {
      case Some((_, minTime)) => {
        def allMins(): List[AwaitCallController] = {
          waiterQueue.headOption match {
            case Some((h, time)) if (ordering(time, minTime) == 0) => {
              waiterQueue.dequeue()
              if (h.checkAlive()) h :: allMins() else allMins()
            }
            case _ => { Nil }
          }
        }
        Some((minTime, allMins()))
      }
      case None => None
    }
  }

  protected def run() {
    synchronized {
      if (readyCount == 0) {
        dequeueMins() match {
          case Some((newTime, first :: rest)) => {
            currentTime = Some(newTime)
            first.publish(true.asInstanceOf[AnyRef])
            rest foreach { _.publish(false.asInstanceOf[AnyRef]) }
          }
          case None => {}
          case x => throw new VirtualClockError("Virtual clock internal failure (dequeueMins return not matched): " + x)
        }
      }
    }
  }

  def setQuiescent() {
    synchronized {
      assert(readyCount > 0, "Virtual clock internal failure: setQuiescent when readyCount = " + readyCount)
      readyCount -= 1
      if (readyCount == 0) {
        runtime.stage(this)
      }
    }
  }

  def unsetQuiescent() {
    synchronized {
      readyCount += 1
    }
  }

  def await(caller: Token, t: Time) {
    val h = new AwaitCallController(caller)
    val timeOrder = synchronized {
      if (readyCount <= 0) {
        h.halt(new VirtualClockError("Virtual clock internal failure: await when readyCount = " + readyCount))
      }
      val order = currentTime map { ordering(t, _) } getOrElse 1
      if (order == 1) waiterQueue += ((h, t))
      order
    }
    timeOrder match {
      // Awaiting a time that has already passed.
      case -1 => caller.halt()

      // Awaiting the current time.
      case 0 => caller.publish(Some(false.asInstanceOf[AnyRef]))

      // Awaiting a future time.
      case 1 => caller.blockOn(h)
    }
  }

  def now(): Option[Time] = synchronized { currentTime }

}
