//
// SwaptionData.scala -- Scala benchmark data SwaptionData
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.swaptions

import java.util.{ Arrays, Random }

import scala.beans.BeanProperty

import orc.test.item.scalabenchmarks.{ BenchmarkConfig, ExpectedBenchmarkResult }

case class Swaption(
    id: Int, factors: Array[Array[Double]], yields: Array[Double],  years: Double, strike: Double, 
    compounding: Double = 0, maturity: Double = 1, tenor: Double = 2, paymentInterval: Double = 1) {
  @BeanProperty
  var simSwaptionPriceMean: Double = 0
  @BeanProperty
  var simSwaptionPriceStdError: Double = 0
  
  val strikeCont = if(compounding == 0.0) {
    strike
  } else {
    (1/compounding)*math.log(1+strike*compounding);  
  }
}

object SwaptionData extends ExpectedBenchmarkResult[Array[Swaption]] {
  def factors() = Array(
      Array(.01, .01, .01, .01, .01, .01, .01, .01, .01, .01),
      Array(.009048, .008187, .007408, .006703, .006065, .005488, .004966, .004493, .004066, .003679),
      Array(.001000, .000750, .000500, .000250, .000000, -.000250, -.000500, -.000750, -.001000, -.001250)
      )
  def yields() = Array.tabulate(nSteps-1)(_ * 0.005 + .1)
  
  val nSteps = 11
  val nTrials = BenchmarkConfig.problemSizeSqrtScaledInt(1000)
  val nSwaptions = BenchmarkConfig.problemSizeSqrtScaledInt(12)

  def makeSwaption(n: Int) = {
    val rnd = new Random(n)
    def random(offset: Double, steps: Int, scale: Double): Double = offset + (rnd.nextDouble() * steps).toInt * scale
    Swaption(n, factors(), yields(), random(5, 60, 0.25), random(0.1, 49, 0.1))
  }
  
  private def sizedData(nSwaptions: Int) = Array.tabulate(nSwaptions)(makeSwaption)
  
  def data = sizedData(nSwaptions)

  override def hash(results: Array[Swaption]): Int = {
    val prec = 1e6
    Arrays.hashCode(results.map(s => (s.simSwaptionPriceMean * prec).toLong * 37 + (s.simSwaptionPriceStdError * prec).toLong))
  }

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x19930ef3,
      10 -> 0x5c3086b8,
      100 -> 0xe7463317,
      )
}
