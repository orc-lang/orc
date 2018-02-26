package orc.test.item.scalabenchmarks.swaptions

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, HashBenchmarkResult }

object SwaptionsSeq extends  BenchmarkApplication[Array[Swaption], Array[Swaption]] with HashBenchmarkResult[Array[Swaption]] {
  val expected = SwaptionData
  def benchmark(data: Array[Swaption]) = {
    val processor = new Processor(SwaptionData.nTrials)
    data.foreach(processor(_))
    data
  }

  def setup(): Array[Swaption] = {
    SwaptionData.data
  }

  val name: String = "Swaptions-seq"

  val size: Int = SwaptionData.nSwaptions * SwaptionData.nTrials
}