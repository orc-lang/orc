//
// Canneal.scala -- Scala benchmark Canneal
// Project OrcTests
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.canneal

import java.net.URL
import java.nio.file.{ Files, Paths }

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, BenchmarkConfig }

object Canneal extends BenchmarkApplication[NetList, Unit] {
  val dataURL = new URL("https://www.cs.utexas.edu/~amp/data/2500000.nets.gz")
  private val localTargetFile = "canneal-input.netlist.gz"
  lazy val localInputFile = {
    val f = Paths.get(localTargetFile)
    if (!Files.isRegularFile(f)) {
      println(s"Downloading $dataURL as test data")
      val in = dataURL.openStream()
      Files.copy(in, f)
      in.close()
    }
    f.toAbsolutePath.toString
  }

  val swapsPerTemp = BenchmarkConfig.problemSizeScaledInt(3750)
  val initialTemperature = 2000
  val filename = localInputFile
  val nTempSteps = 128
  val nPartitions = BenchmarkConfig.nPartitions

  lazy val netlist = NetList(filename)

  def setup(): NetList = {
    netlist.resetLocations()
    netlist
  }

  def benchmark(netlist: NetList): Unit = {
    new Annealer(netlist, nPartitions, swapsPerTemp, initialTemperature, nTempSteps)()
  }

  def check(u: Unit) = false

  val size: Int = nTempSteps * swapsPerTemp

  val name: String = "Canneal"
}
