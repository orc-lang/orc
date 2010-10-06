//
// SupportForVtimer.scala -- Scala class/trait/object SupportForVtimer
// Project OrcScala
//
// $Id$
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
import scala.collection.SortedSet
/**
 * 
 *
 * @author amshali
 */
trait SupportForVtimer extends OrcRuntime {
  
  val tokensRunning : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)
  
  val kCount : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)
  
  def decTokensRunning() {
    val it = tokensRunning.decrementAndGet()
    val ik = kCount.get
    val iz = zSet.size
    if ((it + ik+iz) == 0) {
      scheduleMinVtimer()
    }
  }
  
  def deckCount() {
    val ik = kCount.decrementAndGet()
    val it = tokensRunning.get
    val iz = zSet.size
    if ((it + ik+iz) == 0) {
      scheduleMinVtimer()
    }
  }
  
  val vTime : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)

  case class VTEntry(token: Token, vtime : Int) extends scala.math.Ordered[VTEntry] { 
    def getVtime = vtime;
    def getToken = token
    def compare(o2 : VTEntry) = {
      val d = this.vtime - o2.getVtime
      if (d == 0 && this.getToken == o2.getToken) 0      
      else if (d == 0 || d < 0) -1
      else 1
    } 
  }
  
  var vtSet : SortedSet[VTEntry] = SortedSet[VTEntry]()
  
  var zSet : java.util.Set[Token] = 
    java.util.Collections.synchronizedSet(new java.util.HashSet());
  
  var nzSet : java.util.Set[Token] = 
    java.util.Collections.synchronizedSet(new java.util.HashSet());
  
  def addVtimer(token: Token, n : Int) = synchronized {
    if (n == 0)
      zSet.add(token)
    if (n > 0) {
      nzSet.add(token)
      vtSet = vtSet + VTEntry(token, n)
    }
  }
  def removeVtimer(token: Token) = synchronized {
    zSet.remove(token)
    nzSet.remove(token)
  }

  def scheduleMinVtimer() = synchronized {
    if (vtSet.size > 0) {
      vtSet.firstKey match {
        case VTEntry(token, vtime : Int) => {
          vtSet = vtSet - vtSet.firstKey
          if (nzSet.contains(token)) {
            vtSet = vtSet map {case VTEntry(t1, n1) => VTEntry(t1, n1-vtime)}
            vTime.addAndGet(vtime)
            token.publish(Signal)
          }
        }
      }
    }
  }
  override def getVtime = vTime.get
}