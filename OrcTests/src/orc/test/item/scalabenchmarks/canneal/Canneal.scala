package orc.test.item.scalabenchmarks.canneal

import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.BenchmarkConfig
import java.net.URL
import java.io.File
import java.nio.file.Files

object Canneal extends BenchmarkApplication[NetList, Unit] {
  val dataURL = new URL("https://www.cs.utexas.edu/~amp/data/2500000.nets.gz")
  private val localTargetFile = "canneal-input.netlist.gz"
  lazy val localInputFile = {
    val f = new File(localTargetFile)
    if (!f.isFile()) {
      println(s"Downloading $dataURL as test data")
      val in = dataURL.openStream()
      Files.copy(in, f.toPath())
      in.close()
    }
    f.getAbsolutePath
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