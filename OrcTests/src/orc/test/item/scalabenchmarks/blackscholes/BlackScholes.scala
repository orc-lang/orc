package orc.test.item.scalabenchmarks.blackscholes

import BlackScholes.D
import scala.util.Random
import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.Util
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal
import scala.runtime.ZippedTraversable3.zippedTraversable3ToTraversable

case class BlackScholesStock(price: D, strike: D, maturity: D)
case class BlackScholesResult(call: D, put: D)

object BlackScholesData {
  def seededStream(seed: Long, l: Double, h: Double): Stream[D] = {
    val rand = new Random(seed)
    Stream.continually(rand.nextDouble() * (h - l) + l)
  }

  val dataSize = 100000

  def makeData(s: Int): Array[BlackScholesStock] = {
    (seededStream(1, 0.01, 100), seededStream(2, 0.01, 100), seededStream(3, 0.1, 5)).zipped
      .take(s).map({ case (s, x, t) => BlackScholesStock(s, x, t) }).toArray
  }

  val data = makeData(dataSize)

  val riskless: D = 0.7837868650424492
  val volatility: D = 0.14810754832231288
}

object BlackScholes extends BenchmarkApplication {
  type D = BigDecimal

  def log(x: D): D = Math.log(x.toDouble)
  def sqrt(x: D): D = Math.pow(x.toDouble, 0.5)
  def exp(x: D): D = Math.exp(x.toDouble)

  def round(x: D): Double = x.doubleValue()

  def compute(s: D, x: D, t: D, r: D, v: D): BlackScholesResult = {
    val d1 = round((log(s / x) + (r + v * v / 2) * t) / (v * sqrt(t)))
    val d2 = round(d1 - v * sqrt(t))

    // println((d1.getClass(), d1, d2.getClass(), d2))

    val call = s * cnd(d1) - x * exp(-r * t) * cnd(d2)
    val put = x * exp(-r * t) * cnd(-d2) - s * cnd(-d1)

    BlackScholesResult(call, put)
  }

  val a1: D = 0.31938153
  val a2: D = -0.356563782
  val a3: D = 1.781477937
  val a4: D = -1.821255978
  val a5: D = 1.330274429
  val rsqrt2pi: D = 1.0 / Math.sqrt(2.0 * Math.PI)

  // The cumulative normal distribution function
  def cnd(x: D): D = {
    val l = x.abs;
    val k = round(1.0 / (1.0 + 0.2316419 * l))
    val w = round(1.0 - rsqrt2pi * exp(-l * l / 2) * (a1 * k + a2 * k * k + a3 * k * k * k + a4 * k * k * k * k + a5 * k * k * k * k * k))

    if (x < 0.0) {
      1.0 - w
    } else {
      w
    }
  }

  def main(args: Array[String]) {
    if (args.size == 0) {
      val res = for (BlackScholesStock(s, x, t) <- BlackScholesData.data) yield {
        compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
      }

      println(res.size)
      println(res.take(5).toSeq)
    } else if (args.size == 1) {
      println(BlackScholesData.data.take(5).toSeq)

      val n = args(0).toInt
      for (_ <- 0 until n) {
        Util.timeIt {
          val res = for (BlackScholesStock(s, x, t) <- BlackScholesData.data) yield {
            compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
          }
  
          println(res.size)
          println(res.take(5).toSeq)
        }
      }
    }
  }
}
