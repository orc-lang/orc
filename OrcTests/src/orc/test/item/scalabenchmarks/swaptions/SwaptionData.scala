package orc.test.item.scalabenchmarks.swaptions

import java.util.concurrent.ThreadLocalRandom
import scala.beans.BeanProperty
import orc.test.item.scalabenchmarks.BenchmarkConfig

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

object SwaptionData {
  def factors() = Array(
      Array(.01, .01, .01, .01, .01, .01, .01, .01, .01, .01),
      Array(.009048, .008187, .007408, .006703, .006065, .005488, .004966, .004493, .004066, .003679),
      Array(.001000, .000750, .000500, .000250, .000000, -.000250, -.000500, -.000750, -.001000, -.001250)
      )
  def yields() = Array.tabulate(nSteps-1)(_ * 0.005 + .1)
  
  var nextSwaptionID = 0
  
  val nSteps = 11
  val nTrials = BenchmarkConfig.problemSizeSqrtScaledInt(1000)
  val nSwaptions = BenchmarkConfig.problemSizeSqrtScaledInt(12)

  def makeSwaption() = synchronized {
    val rnd = ThreadLocalRandom.current()
    def random(offset: Double, steps: Int, scale: Double): Double = offset + (rnd.nextDouble() * steps).toInt * scale
    val id = nextSwaptionID
    nextSwaptionID += 1
    Swaption(id, factors(), yields(), random(5, 60, 0.25), random(0.1, 49, 0.1))
  }
  
  private def sizedData(nSwaptions: Int) = Array.fill(nSwaptions)(makeSwaption())
  
  def data = sizedData(nSwaptions)
}