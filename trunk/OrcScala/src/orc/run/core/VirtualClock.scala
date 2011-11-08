//
// VirtualClock.scala -- Scala class VirtualClock, trait VirtualClockOperation, and class AwaitCallHandle
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import scala.collection.mutable

import orc.{Schedulable, OrcRuntime}
import orc.values.sites.{SpecificArity, Site}

/**
  * @author dkitchin
  */
trait VirtualClockOperation extends Site with SpecificArity

/**
  * @author dkitchin
  */
class AwaitCallHandle(caller: Token) extends CallHandle(caller) {

  override def toString() = "ach"
}

/**
  * @author dkitchin
  */
class VirtualClock(val parent: Option[VirtualClock] = None, ordering: (AnyRef, AnyRef) => Int, runtime: OrcRuntime)
  extends Schedulable {

  override val nonblocking = true

  type Time = AnyRef

  val queueOrder = new Ordering[(AwaitCallHandle, Time)] {
    def compare(x: (AwaitCallHandle, Time), y: (AwaitCallHandle, Time)) = ordering(y._2, x._2)
  }

  var currentTime: Option[Time] = None
  val waiterQueue: mutable.PriorityQueue[(AwaitCallHandle, Time)] = new mutable.PriorityQueue()(queueOrder)

  private var readyCount: Int = 1

  /*
   * Take all minimum time elements from the waiter queue.
   * Return Some tuple contaning the minimum time and a nonempty list of waiters on that time.
   * If the queue is empty, return None instead.
   */
  private def dequeueMins(): Option[(Time, List[AwaitCallHandle])] = {
    waiterQueue.headOption match {
      case Some((_, minTime)) => {
        def allMins(): List[AwaitCallHandle] = {
          waiterQueue.headOption match {
            case Some((h, time)) if (ordering(time, minTime) == 0) => {
              waiterQueue.dequeue()
              h :: allMins()
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
        }
      }
    }
  }

  def setQuiescent() {
    synchronized {
      assert(readyCount > 0)
      readyCount -= 1
      if (readyCount == 0) {
        parent foreach { _.setQuiescent() }
        runtime.schedule(this)
      }
    }
  }

  def unsetQuiescent() {
    synchronized {
      readyCount += 1
      if (readyCount == 1) {
        parent foreach { _.unsetQuiescent() }
      }
    }
  }

  def await(caller: Token, t: Time) {
    val h = new AwaitCallHandle(caller)
    val timeOrder = synchronized {
      assert(readyCount > 0)
      val order = currentTime map { ordering(t, _) } getOrElse 1
      if (order == 1) { waiterQueue += ((h, t)) }
      order
    }
    timeOrder match {
      // Awaiting a time that has already passed.
      case -1 => caller.halt()

      // Awaiting the current time.
      case 0 => caller.publish(false.asInstanceOf[AnyRef])

      // Awaiting a future time.
      case 1 => caller.blockOn(h)
    }
  }

  def now(): Option[Time] = synchronized { currentTime }

}
