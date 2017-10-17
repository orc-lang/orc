package orc.test.item.scalabenchmarks.swaptions

import orc.test.item.scalabenchmarks.Util

object SwaptionsSeq {
  def main(args: Array[String]): Unit = {
    val data = SwaptionData.sizedData(64)
    val processor = new SwaptionProcessor(SwaptionData.nTrials)
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