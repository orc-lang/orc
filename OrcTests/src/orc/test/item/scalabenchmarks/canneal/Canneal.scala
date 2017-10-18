package orc.test.item.scalabenchmarks.canneal

import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.BenchmarkConfig

object Canneal extends BenchmarkApplication[NetList] {
  val swapsPerTemp = BenchmarkConfig.problemSizeScaledInt(15000)
  val initialTemperature = 2000 
  // FIXME: Generate or include data.
  val filename = "/home/amp/Redownloadable/parsec-3.0/pkgs/kernels/canneal/inputs/2500000.nets"
  val nTempSteps = 128
  val nPartitions = 8

  val netlist = NetList(filename)    
  
  def setup(): NetList = {
    netlist.resetLocations()
    netlist
  }

  def benchmark(netlist: NetList): Unit = {
    new Annealer(netlist, nPartitions, swapsPerTemp, initialTemperature, nTempSteps)() 
  }

  val size: Int = nTempSteps * swapsPerTemp

  val name: String = "Canneal"
}