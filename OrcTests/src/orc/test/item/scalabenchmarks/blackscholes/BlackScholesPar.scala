package orc.test.item.scalabenchmarks.blackscholes

import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.Util

object BlackScholesPar extends BenchmarkApplication[Array[BlackScholesStock]] {
  def benchmark(data: Array[BlackScholesStock]): Unit = {
    for (BlackScholesStock(s, x, t) <- data.par) yield {
      BlackScholes.compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
    }
  }

  def setup(): Array[BlackScholesStock] = {
    BlackScholesData.data
  }

  val name: String = "Black-Scholes-par"

  val size: Int = BlackScholesData.data.size
}
