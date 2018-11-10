//
// BlackScholes.scala -- Scala benchmarks and components for BlackScholes
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.blackscholes

//import scala.math.BigDecimal.int2bigDecimal
import scala.runtime.ZippedTraversable3.zippedTraversable3ToTraversable
import scala.util.Random

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, BenchmarkConfig, ExpectedBenchmarkResult, HashBenchmarkResult }

import BlackScholes.D

case class BlackScholesStock(price: D, strike: D, maturity: D)
case class BlackScholesResult(var call: D, var put: D) {
  override def hashCode(): Int = {
    val prec = 1e6
    (call * prec).toLong.## * 37 ^ (put * prec).toLong.##
  }
}

object BlackScholesData extends ExpectedBenchmarkResult[Array[BlackScholesResult]] {
  def seededStream(seed: Long, l: Double, h: Double): Stream[D] = {
    val rand = new Random(seed)
    Stream.continually(rand.nextDouble() * (h - l) + l)
  }

  val dataSize = BenchmarkConfig.problemSizeScaledInt(50000)

  private def makeData(s: Int): Array[BlackScholesStock] = {
    (seededStream(1, 0.01, 100), seededStream(2, 0.01, 100), seededStream(3, 0.1, 5)).zipped
      .take(s).map({ case (s, x, t) => BlackScholesStock(s, x, t) }).toArray
  }

  lazy val data = makeData(dataSize)

  val riskless: D = 0.7837868650424492
  val volatility: D = 0.14810754832231288

  val expectedMap: Map[Int, Int] = Map(
      1   -> 0xf52fcf7,
      10  -> 0x62c42cc1,
      100 -> 0xb8277521,
      )

}

object BlackScholes extends BenchmarkApplication[Array[BlackScholesStock], Array[BlackScholesResult]] with HashBenchmarkResult[Array[BlackScholesResult]] {
  val expected: ExpectedBenchmarkResult[Array[BlackScholesResult]] = BlackScholesData

  type D = Double

  def log(x: D): D = Math.log(x.toDouble)
  def sqrt(x: D): D = Math.pow(x.toDouble, 0.5)
  def exp(x: D): D = Math.exp(x.toDouble)

  def round(x: D): Double = x //.doubleValue()

  // Lines: 6
  def compute(s: D, x: D, t: D, r: D, v: D): BlackScholesResult = {
    val d1 = round((log(s / x) + (r + v * v / 2) * t) / (v * sqrt(t)))
    val d2 = round(d1 - v * sqrt(t))

    // println((d1.getClass(), d1, d2.getClass(), d2))

    val call = s * cnd(d1) - x * exp(-r * t) * cnd(d2)
    val put = x * exp(-r * t) * cnd(-d2) - s * cnd(-d1)

    BlackScholesResult(call, put)
  }

  // Lines: 6
  val a1: D = 0.31938153
  val a2: D = -0.356563782
  val a3: D = 1.781477937
  val a4: D = -1.821255978
  val a5: D = 1.330274429
  val rsqrt2pi: D = 1.0 / sqrt(2.0 * Math.PI)

  // Lines: 8
  // The cumulative normal distribution function
  def cnd(x: D): D = {
    val l = math.abs(x)
    val k = round(1.0 / (1.0 + 0.2316419 * l))
    val w = round(1.0 - rsqrt2pi * Math.exp(-l * l / 2) * (a1 * k + a2 * k * k + a3 * k * k * k + a4 * k * k * k * k + a5 * k * k * k * k * k))

    if (x < 0.0) {
      1.0 - w
    } else {
      w
    }
  }

  def benchmark(data: Array[BlackScholesStock]) = {
    for (BlackScholesStock(s, x, t) <- data) yield {
      compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
    }
  }

  def setup(): Array[BlackScholesStock] = {
    BlackScholesData.data
  }

  val name: String = "Black-Scholes-naive"

  lazy val size: Int = BlackScholesData.data.size
}
