package orc.test.item.scalabenchmarks.swaptions

import orc.test.item.scalabenchmarks.BenchmarkApplication

object SwaptionsParSwaption extends BenchmarkApplication[Array[Swaption]] {
  def benchmark(data: Array[Swaption]): Unit = {
    val processor = new Processor(SwaptionData.nTrials)
    data.par.map(processor(_))
  }

  def setup(): Array[Swaption] = {
    SwaptionData.data
  }

  val name: String = "Swaptions-par-swaption"

  val size: Int = SwaptionData.nSwaptions * SwaptionData.nTrials
}

object SwaptionsParTrial extends BenchmarkApplication[Array[Swaption]] {
  def benchmark(data: Array[Swaption]): Unit = {
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
    data.map(processor(_))
  }

  def setup(): Array[Swaption] = {
    SwaptionData.data
  }

  val name: String = "Swaptions-par-trial"

  val size: Int = SwaptionData.nSwaptions * SwaptionData.nTrials
}
