//
// SupportForVtimer.scala -- Scala trait SupportForVtimer
// Project OrcScala
//
// $Id: SupportForVtimer.scala 2329 2011-01-14 20:55:15Z dkitchin $
//
// Created by amshali on Oct 1, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.values.Signal
import orc.OrcRuntime
import orc.Handle
import scala.collection.SortedSet
/**
 * 
 *
 * @author amshali
 */
trait SupportForVtimer extends OrcRuntime {
  
  val handlesRunning : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)
  
  def decHandlesRunning() {
    val it = handlesRunning.decrementAndGet()
    val iz = zSet.size
    if ((it + iz) == 0) {
      scheduleMinVtimer()
    }
  }
  
  val vTime : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)

  case class VTEntry(handle: Handle, vtime : Int) extends scala.math.Ordered[VTEntry] { 
    def getVtime = vtime;
    def getHandle = handle
    def compare(o2 : VTEntry) = {
      val d = this.vtime - o2.getVtime
      if (d == 0 && this.getHandle == o2.getHandle) 0      
      else if (d == 0 || d < 0) -1
      else 1
    } 
  }
  
  var vtSet : SortedSet[VTEntry] = SortedSet[VTEntry]()
  
  var zSet : java.util.Set[Handle] = 
    java.util.Collections.synchronizedSet(new java.util.HashSet());
  
  var nzSet : java.util.Set[Handle] = 
    java.util.Collections.synchronizedSet(new java.util.HashSet());
  
  def addVtimer(handle: Handle, n : Int) = synchronized {
    if (n == 0)
      zSet.add(handle)
    if (n > 0) {
      nzSet.add(handle)
      vtSet = vtSet + VTEntry(handle, n)
    }
  }
  def removeVtimer(handle: Handle) = synchronized {
    zSet.remove(handle)
    nzSet.remove(handle)
  }

  def scheduleMinVtimer() = synchronized {
    if (vtSet.size > 0) {
      vtSet.firstKey match {
        case VTEntry(handle, vtime : Int) => {
          vtSet = vtSet - vtSet.firstKey
          if (nzSet.contains(handle)) {
            vtSet = vtSet map {case VTEntry(t1, n1) => VTEntry(t1, n1-vtime)}
            vTime.addAndGet(vtime)
            handle.publish(Signal)
          }
        }
      }
    }
  }
  def getVtime = vTime.get
}