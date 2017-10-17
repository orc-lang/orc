package orc.test.item.scalabenchmarks.swaptions

import java.util.concurrent.ThreadLocalRandom

class Processor(nTrials: Int) {  
  import SwaptionData._
  
  def apply(swaption: Swaption): Unit = {
    val results = Array.fill(nTrials)(simulate(swaption))
    val sum = results.sum
    val sumsq = results.map(v => v*v).sum
    swaption.simSwaptionPriceMean = sum / nTrials
    swaption.simSwaptionPriceStdError = math.sqrt((sumsq - sum*sum/nTrials) / (nTrials - 1.0)) / math.sqrt(nTrials)
  }

  def simulate(swaption: Swaption): Double = {
    // Setup:
    
    val timeDelta = swaption.years / nSteps
    val freqRatio = (swaption.paymentInterval / timeDelta).ceil.toInt
    val swapPathStart = (swaption.maturity / timeDelta).ceil.toInt
    val swapTimePoints = (swaption.tenor / timeDelta).ceil.toInt
    val swapPathLength = nSteps - swapPathStart
    val swapPathYears = swapPathLength * timeDelta
    
    val swapPayoffs = Array.tabulate[Double](swapPathLength) { i =>
      if (i == 0)
        0
      else if (i % freqRatio == 0) {
        val offset = if (i == swapTimePoints) 0 else -1
        math.exp(swaption.strikeCont * swaption.paymentInterval) + offset
      } else {
        0
      }
    }
    
    val forwards = yieldToForward(swaption.yields)
    
    val totalDrift = computeDrifts(swaption)

    val path = simPathForward(swaption, forwards, totalDrift)

    val discountingRatePath = Array.tabulate(nSteps)(i => path(i)(0))
    val payoffDiscountFactors = discountFactors(nSteps, swaption.years, discountingRatePath)
    val swapRatePath = path(swapPathStart)
    val swapDiscountFactors = discountFactors(swapPathLength, swapPathYears, swapRatePath)
    
    val fixedLegValue = (swapPayoffs.view zip swapDiscountFactors.view).map({ case (x, y) => x * y }).sum
    
    val swaptionPayoff = (fixedLegValue - 1) max 0
    val discSwaptionPayoff = swaptionPayoff * payoffDiscountFactors(swapPathStart)
    
    discSwaptionPayoff
  }

  // This function computes forward rates from supplied yield rates.
  def yieldToForward(yields: Array[Double]): Array[Double] = {
    val head = yields(0)
    val tail = yields.view.sliding(2).zipWithIndex.map { p => 
      val now = p._1(1)
      val last = p._1(0)
      val i = p._2
      (i-1)*now - i*last
    }
    Array(head) ++ tail
  }

  def computeDrifts(swaption: Swaption): Array[Double] = {
    val timeDelta = swaption.years / nSteps
    val drifts = Array.ofDim[Double](swaption.factors.size, nSteps - 1)
    
    // TODO: There is probably a clean way to implement this as a single scanLeft operation.
    
    for (i <- 0 until swaption.factors.size) {
      drifts(i)(0) = 0.5 * timeDelta * swaption.factors(i)(0) * swaption.factors(i)(0)
    }
    
    for (i <- 0 until swaption.factors.size; j <- 1 until nSteps - 1) {
      val prevDrift = (0 until j).map(l => drifts(i)(l)).sum
      val prevFactor = (0 until j+1).map(l => swaption.factors(i)(l)).sum
      
      drifts(i)(j) = -prevDrift + 0.5 * timeDelta * prevFactor * prevFactor
    }
    
    (for (i <- 1 until nSteps - 1) yield {
      (0 until swaption.factors.size).map(j => drifts(j)(i)).sum
    }).toArray
  }

  def simPathForward(swaption: Swaption, forwards: Array[Double], totalDrift: Array[Double]): Array[Array[Double]] = {
    val timeDelta = swaption.years / nSteps
    val path = Array.ofDim[Double](nSteps, nSteps)
    path(0) = forwards

    for (j <- 1 until nSteps) {
      val shocks = Array.fill(swaption.factors.size)(CumNormalInv(ThreadLocalRandom.current().nextDouble()))
      for (l <- 0 until nSteps - (j + 1)) {
        val totalShock = (0 until swaption.factors.size).map(i => swaption.factors(i)(l) * shocks(i)).sum
        path(j)(l) = path(j-1)(l+1) + totalDrift(l) + math.sqrt(timeDelta) * totalShock
      }
    }
    
    path
  }

  def discountFactors(nSteps: Int, years: Double, path: Array[Double]): Array[Double] = {
    val timeDelta = years / nSteps
    
    val discountFactors = Array.fill(nSteps)(1.0)
    
    for (i <- 1 until nSteps; j <- 0 until i) {
      discountFactors(i) *= math.exp(-path(j) * timeDelta)
    }
    

    discountFactors
  }
}
