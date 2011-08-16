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

/**
 * 
 *
 * @author dkitchin
 */

class VirtualClock(val parent: Option[VirtualClock] = None, ordering: (AnyRef, AnyRef) => Int) {

  type Time = AnyRef
  
  val queueOrder = new Ordering[(Handle,Time)] {
    def compare(x: (Handle,Time), y: (Handle,Time)) = ordering(y._2, x._2)
  } 
      
  var currentTime: Option[Time] = None 
  val waiterQueue: mutable.PriorityQueue[(Handle,Time)] = new mutable.PriorityQueue()(queueOrder)
  
  private var readyCount: Int = 1
  
  protected def advance() {      
    waiterQueue.headOption match {
      case Some((_, minimumTime)) => {
        currentTime = Some(minimumTime)
        def atMinimum(entry: (Handle, Time)) = ordering(entry._2, minimumTime) == 0
        val allMins = waiterQueue takeWhile atMinimum
        allMins foreach { _ => waiterQueue.dequeue() }
        allMins foreach { entry => entry._1.publish() } 
      }
      case None => { }
    }
  }
  
  def setQuiescent() {
    synchronized {
      assert(readyCount > 0)
      readyCount -= 1
      parent foreach { _.setQuiescent() }
      if (readyCount == 0) advance()
    }
  }
  
  def unsetQuiescent() { synchronized { readyCount += 1 } }
  
  def await(h: Handle, t: Time): Boolean = {
    currentTime match {
      case None => {
        waiterQueue += ( (h,t) )
        if (readyCount == 0) advance()
        true
      }
      case Some(ct) => {
        ordering(t, ct) match {
          // t is earlier than the current time
          case -1 => h.halt ; false
          
          // t is at the current time
          case 0 => h.publish() ; false
          
          // t is later than the current time
          case 1 => {
            waiterQueue += ( (h,t) )
            if (readyCount == 0) advance()
            true
          }
        }
      }
    }
  }
  
  def now(): Option[Time] = synchronized { currentTime }
  
} 