//
// Annealer.scala -- Scala benchmark component Annealer
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.canneal

import java.util.concurrent.{ CyclicBarrier, ThreadLocalRandom }

import scala.annotation.tailrec

class Annealer(
    val netlist: NetList,
    val nThreads: Int,
    val swapsPerTemp: Int,
    val initialTemperature: Double,
    val nTempSteps: Int = -1) {

  val movesPerThreadTemp: Int = swapsPerTemp / nThreads
  val barrier = new CyclicBarrier(nThreads)
  // TODO: continueFlag is really racy. Other threads could easily do another temp step. But this is what Parsec does.
  var continueFlag = true
  
  def apply(): Unit = {
    val threads = for (_ <- 0 until nThreads) yield  {
      new Thread(() => threadBody())
    }
    
    threads foreach { _.start() }
    threads foreach { _.join() }
  }
  
  private def threadBody(): Unit = {
    implicit val rng = ThreadLocalRandom.current()
    var lastElem: Element = netlist.randomElement()
    
    @inline
    def keepGoing(tempStep: Int, acceptedGood: Int, acceptedBad: Int): Boolean = {
      if (nTempSteps < 0) {
        val r = continueFlag && acceptedGood > acceptedBad
        if (!r)
          continueFlag = false
        r
      } else {
        tempStep < nTempSteps
      }
    }
    
    @inline
    def swapCost(a: Element, b: Element): Double = {
      val aLoc = a.location.get()
      val bLoc = b.location.get()
      a.swapCost(aLoc, bLoc) + b.swapCost(bLoc, aLoc)
    }
    
    @tailrec
    def anneal(temperature: Double, tempStep: Int): Unit = {
      var acceptedGood: Int = 0
      var acceptedBad: Int = 0
      
      for (i <- 0 until movesPerThreadTemp) {
        val a = lastElem
        val b = netlist.randomElement(a)
        lastElem = b
        val cost = swapCost(a, b)
        if (cost < 0) { 
          // Good swap
          a.location.swap(b.location)
          acceptedGood += 1
        } else {
          val boltsman = Math.exp(- cost / temperature)
          if (rng.nextDouble() < boltsman) {
            // Accepted bad swap
            a.location.swap(b.location)
            acceptedBad += 1            
          } else {
            // Rejected bad swap
          }
        }
      }
      
      barrier.await()
      
      if (keepGoing(tempStep, acceptedGood, acceptedBad)) {
        anneal(temperature / 1.5, tempStep + 1)
      }
    }
    
    anneal(initialTemperature, 0)
  }
}
