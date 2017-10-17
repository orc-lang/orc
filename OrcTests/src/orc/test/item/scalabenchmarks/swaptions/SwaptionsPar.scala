package orc.test.item.scalabenchmarks.swaptions

import orc.test.item.scalabenchmarks.Util

object SwaptionsParSwaption {
  def main(args: Array[String]): Unit = {
    val data = SwaptionData.sizedData(SwaptionData.nSwaptions)
    val processor = new Processor(SwaptionData.nTrials)
    if (args.size == 0) {
      data.par.map(processor(_))
      //println((data(0).simSwaptionPriceMean, data(0).simSwaptionPriceStdError))
    } else if (args.size == 1) {
      val n = args(0).toInt
      for (_ <- 0 until n) {
        Util.timeIt {
          data.par.map(processor(_))
        }
        println((data(0).simSwaptionPriceMean, data(0).simSwaptionPriceStdError))
      }
    }
  }
}

object SwaptionsParTrial {
  def main(args: Array[String]): Unit = {
    val data = SwaptionData.sizedData(SwaptionData.nSwaptions)
    val processor = new Processor(SwaptionData.nTrials) {
      import SwaptionData._
      override def apply(swaption: Swaption): Unit = {
        val results = (0 until nTrials).toArray.par.map(_ => simulate(swaption))
        val sum = results.sum
        val sumsq = results.map(v => v*v).sum
        swaption.simSwaptionPriceMean = sum / nTrials
        swaption.simSwaptionPriceStdError = math.sqrt((sumsq - sum*sum/nTrials) / (nTrials - 1.0)) / math.sqrt(nTrials)
      }
    }
    if (args.size == 0) {
      data.map(processor(_))
      //println((data(0).simSwaptionPriceMean, data(0).simSwaptionPriceStdError))
    } else if (args.size == 1) {
      val n = args(0).toInt
      for (_ <- 0 until n) {
        Util.timeIt {
          data.map(processor(_))
        }
        println((data(0).simSwaptionPriceMean, data(0).simSwaptionPriceStdError))
      }
    }
  }
}
