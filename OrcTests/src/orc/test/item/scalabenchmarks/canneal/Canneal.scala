package orc.test.item.scalabenchmarks.canneal

import orc.test.item.scalabenchmarks.Util

object Canneal {
  object IntFromString {
    def unapply(s: String): Option[Int] = {
      if (s != null)
        Some(s.toInt)
      else
        None
    }
  }
  
  def main(args: Array[String]): Unit = {
    val Seq(IntFromString(nThreads), IntFromString(swapsPerTemp), IntFromString(startTemp), filename, IntFromString(nTempSteps)) = args.toSeq
    val netlist = NetList(filename)    
    
    val n = 25

    for (_ <- 0 until n) {
      netlist.resetLocations()
      Util.timeIt {
        new Annealer(netlist, nThreads, swapsPerTemp, startTemp, nTempSteps)() 
      }
      println(s"total cost = ${netlist.totalCost().toLong}")
    }
  }
}