//
// SwaptionsSeq.scala -- Scala benchmark SwaptionsSeq
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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
