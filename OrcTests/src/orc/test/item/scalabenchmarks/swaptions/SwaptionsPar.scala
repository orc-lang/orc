package orc.test.item.scalabenchmarks.swaptions

import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.HashBenchmarkResult

object SwaptionsParSwaption extends BenchmarkApplication[Array[Swaption], Array[Swaption]] with HashBenchmarkResult[Array[Swaption]] {
  val expected = SwaptionData
  
  def benchmark(data: Array[Swaption]) = {
    val processor = new Processor(SwaptionData.nTrials)
    data.par.foreach(processor(_))
    data
  }

  def setup(): Array[Swaption] = {
    SwaptionData.data
  }

  val name: String = "Swaptions-par-swaption"

  val size: Int = SwaptionData.nSwaptions * SwaptionData.nTrials

}

object SwaptionsParTrial extends BenchmarkApplication[Array[Swaption], Array[Swaption]] with HashBenchmarkResult[Array[Swaption]] {
  val expected = SwaptionData
  
  def benchmark(data: Array[Swaption]) = {
    val processor = new Processor(SwaptionData.nTrials) {
      import SwaptionData._
      override def apply(swaption: Swaption): Unit = {
        val results = (0 until nTrials).toArray.par.map(simulate(swaption, _))
        val sum = results.sum
        val sumsq = results.map(v => v*v).sum
        swaption.simSwaptionPriceMean = sum / nTrials
        swaption.simSwaptionPriceStdError = math.sqrt((sumsq - sum*sum/nTrials) / (nTrials - 1.0)) / math.sqrt(nTrials)
      }
    }
    data.foreach(processor(_))
    data
  }

  def setup(): Array[Swaption] = {
    SwaptionData.data
  }

  val name: String = "Swaptions-par-trial"

  val size: Int = SwaptionData.nSwaptions * SwaptionData.nTrials
}
