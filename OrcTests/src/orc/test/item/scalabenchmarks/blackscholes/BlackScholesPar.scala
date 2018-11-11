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

object BlackScholesPar extends BenchmarkApplication[Array[BlackScholesStock], Array[BlackScholesStock]] with HashBenchmarkResult[Array[BlackScholesStock]] {
  val expected = BlackScholesData

  // Lines: 4
  def benchmark(data: Array[BlackScholesStock]) = {
    for (s <- data.par) {
      BlackScholes.compute(s, BlackScholesData.riskless, BlackScholesData.volatility)
    }
    data
  }

  def setup(): Array[BlackScholesStock] = {
    BlackScholesData.data
  }

  val name: String = "Black-Scholes-par"

  lazy val size: Int = BlackScholesData.data.size
}
