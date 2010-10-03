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
import orc.TokenAPI
/**
 * 
 *
 * @author amshali
 */
trait SupportForVtimer extends OrcRuntime {
  
  /*
   * OK! Once upon a time there was an engine called Orc!
   * This engine used to run things. But sometimes she didn't
   * have anything to run and the world was so silent for 
   * Orc! The poor Orc didn't like the silence of the world!
   * She was sad until one day when a new site came along! 
   * The Vtimer! Vtimer asked "why are you sad Orc?" Orc said: 
   * "World is so silent and I am bored! I have nothing to 
   * do!" Vtimer cared a lot about Orc and wanted to do 
   * something for her. He started to think and suddenly 
   * a great idea came to his mind! "You can run me! Yeah run me
   * when the world is silent!", Vtimer said! Orc was happy!
   * "But how do I know the world is silent?" Orc asked. Vtimer
   * answered: "Just keep track of the number of tokens running
   * and number of continuations running and when they are both 
   * zero you know that world is silent and then you can run me!"
   */
  
  val tokensRunning : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)
  
  val kCount : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)
  
  def decTokensRunning() {
    val it = tokensRunning.decrementAndGet()
    val ik = kCount.get
    if ((it + ik) == 0) {
      scheduleMinVtimer()
    }
  }
  
  def deckCount() {
    val ik = kCount.decrementAndGet()
    val it = tokensRunning.get
    if ((it + ik) == 0) {
      scheduleMinVtimer()
    }
  }
  
  override def scheduleVtimer(t: TokenAPI, vtime : Int) {
    addVtimer(t, vtime)
  }
  
  val vTime : java.util.concurrent.atomic.AtomicInteger = 
    new java.util.concurrent.atomic.AtomicInteger(0)

  case class VTEntry(token: TokenAPI, vtime : Int) extends scala.math.Ordered[VTEntry] { 
    def getVtime = vtime;
    def compare(o2 : VTEntry) = {this.vtime - o2.getVtime} 
  }
  
  var vtSet : SortedSet[VTEntry] = SortedSet[VTEntry]()
  
  def addVtimer(token: TokenAPI, n : Int) = synchronized {
    vtSet = vtSet + VTEntry(token, n)
  }
  def scheduleMinVtimer() = synchronized {
    if (vtSet.size > 0) {
      vtSet.firstKey match {
        case VTEntry(token: TokenAPI, vtime : Int) => {
          if (token.isLive) {
            vtSet = vtSet - vtSet.firstKey
            vTime.addAndGet(vtime)
            token.publish(Signal)
          }
        }
      }
    }
  }
  override def getVtime = vTime.get
}