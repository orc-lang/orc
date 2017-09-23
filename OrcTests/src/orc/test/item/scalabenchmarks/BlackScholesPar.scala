package orc.test.item.scalabenchmarks

object BlackScholesPar extends BenchmarkApplication {

  def main(args: Array[String]) {
    if (args.size == 0) {
      val res = for (BlackScholesStock(s, x, t) <- BlackScholesData.data.par) yield {
        BlackScholes.compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
      }

      println(res.size)
      println(res.take(5).toSeq)
    } else if (args.size == 1) {
      println(BlackScholesData.data.take(5).toSeq)

      val n = args(0).toInt
      for (_ <- 0 until n) {
        Util.timeIt {
          val res = for (BlackScholesStock(s, x, t) <- BlackScholesData.data.par) yield {
            BlackScholes.compute(s, x, t, BlackScholesData.riskless, BlackScholesData.volatility)
          }
  
          println(res.size)
          println(res.take(5).toSeq)
        }
      }
    }
  }
}
