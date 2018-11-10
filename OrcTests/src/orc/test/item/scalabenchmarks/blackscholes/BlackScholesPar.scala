//
// BlackScholesPar.scala -- Scala benchmark BlackScholesPar
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.blackscholes

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, HashBenchmarkResult }

// FIXME: This does not scale the way it should. Possible reasons are:
//    GC overhead due to allocating two 50-70MB objects for every run
//    Overhead in Scala parallel collections

object BlackScholesPar extends BenchmarkApplication[Array[BlackScholesStock], Array[BlackScholesResult]] with HashBenchmarkResult[Array[BlackScholesResult]] {
  val expected = BlackScholesData

  // Lines: 4
  def benchmark(data: Array[BlackScholesStock]) = {
    //val out = Array.ofDim[BlackScholesResult](data.length)
    val res = for (BlackScholesStock(s, x, t) <- data.par) yield {
      BlackScholes.compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
    }
    res.toArray
  }

  def setup(): Array[BlackScholesStock] = {
    BlackScholesData.data
  }

  val name: String = "Black-Scholes-par"

  lazy val size: Int = BlackScholesData.data.size
}
