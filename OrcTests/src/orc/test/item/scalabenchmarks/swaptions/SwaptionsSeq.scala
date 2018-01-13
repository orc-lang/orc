package orc.test.item.scalabenchmarks.swaptions

import orc.test.item.scalabenchmarks.Util
import orc.test.item.scalabenchmarks.BenchmarkApplication

object SwaptionsSeq extends BenchmarkApplication[Array[Swaption]] {
  def benchmark(data: Array[Swaption]): Unit = {
    val processor = new Processor(SwaptionData.nTrials)
    data.map(processor(_))
  }

  def setup(): Array[Swaption] = {
    SwaptionData.data
  }

  val name: String = "Swaptions-seq"

  val size: Int = SwaptionData.nSwaptions * SwaptionData.nTrials
}