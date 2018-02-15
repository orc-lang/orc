package orc.test.item.scalabenchmarks.blackscholes

import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.Util
import scala.collection.parallel.mutable.ParArray
import orc.test.item.scalabenchmarks.HashBenchmarkResult

object BlackScholesPar extends BenchmarkApplication[Array[BlackScholesStock], Array[BlackScholesResult]] with HashBenchmarkResult[Array[BlackScholesResult]] {
  val expected = BlackScholesData
  
  def benchmark(data: Array[BlackScholesStock]) = {
    val res = for (BlackScholesStock(s, x, t) <- data.par) yield {
      BlackScholes.compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
    }
    res.toArray
  }

  def setup(): Array[BlackScholesStock] = {
    BlackScholesData.data
  }

  val name: String = "Black-Scholes-par"

  val size: Int = BlackScholesData.data.size
}
