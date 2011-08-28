//
// VirtualClock.scala -- Scala class/trait/object VirtualClock
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

import orc.Handle
import orc.lib.time._
import scala.collection.mutable
import orc.values.sites.SpecificArity
import orc.Schedulable
import orc.values.sites.Site
import orc.OrcRuntime

/**
 * 
 *
 * @author dkitchin
 */


trait VirtualClockOperation extends Site with SpecificArity

class VirtualClock(val parent: Option[VirtualClock] = None, ordering: (AnyRef, AnyRef) => Int, runtime: OrcRuntime)
extends Schedulable {

  override val nonblocking = true
  
  type Time = AnyRef
  
  val queueOrder = new Ordering[(Handle,Time)] {
    def compare(x: (Handle,Time), y: (Handle,Time)) = ordering(y._2, x._2)
  } 
      
  var currentTime: Option[Time] = None 
  val waiterQueue: mutable.PriorityQueue[(Handle,Time)] = new mutable.PriorityQueue()(queueOrder)
  
  private var readyCount: Int = 1
  
  protected def run() {
    synchronized {
      if (readyCount == 0) {
        waiterQueue.headOption match {
          case Some((_, minimumTime)) => {
            currentTime = Some(minimumTime)
            def atMinimum(entry: (Handle, Time)) = ordering(entry._2, minimumTime) == 0
            val allMins = waiterQueue takeWhile atMinimum
            allMins foreach { _ => waiterQueue.dequeue() }
            allMins.head._1.publish(true.asInstanceOf[AnyRef])
            allMins.tail foreach { entry => entry._1.publish(false.asInstanceOf[AnyRef]) }
          }
          case None => { }
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
      if (readyCount == 0) {
        parent foreach { _.unsetQuiescent() }
      }
      readyCount += 1 
    } 
  }
  
  def await(caller: Token, t: Time) {
    val h = new ClockCallHandle(caller)
    val timeOrder = synchronized {
      assert(readyCount > 0)
      val order = currentTime map { ordering(t, _) } getOrElse 1
      if (order == 1) { waiterQueue += ( (h,t) ) } 
      order
    }
    timeOrder match {
      // Awaiting a time that has already passed.
      case -1 => caller.halt()
      
      // Awaiting the current time.
      case 0 => caller.publish()
      
      // Awaiting a future time.
      case 1 => caller.blockOn(h)
    }
  }
  
  def now(): Option[Time] = synchronized { currentTime }
  
} 