package orc.test.util

import java.io.{ BufferedWriter, File, FileOutputStream, OutputStreamWriter }

import scala.io.Source

object ProcessOptimizerLog {

  val BenchmarkHeader = """Benchmarking ([^ ]+)""".r.unanchored
  val OrctimizerLine = """Orctimizer before pass ([0-9]+)/[0-9]+:.*Force=([0-9]+).*Resolve=([0-9]+).*Future=([0-9]+).*""".r.unanchored
  val PorcLine = """Porc optimization pass ([0-9]+)/[0-9]+:.*forces = ([0-9]+).*closures = ([0-9]+).*spawns = ([0-9]+).*indirect calls = ([0-9]+).*direct calls = ([0-9]+).*""".r.unanchored
  
  def main(args: Array[String]): Unit = {
    val inputFile = new File(args(0))
    val outputFile = new File(args(0).stripSuffix(".log") + "_optdata.tsv")
    var currentBenchmark = "NONE"
    
    val out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)))
    
    val source = Source.fromFile(inputFile)
    for(l <- source.getLines()) {
      l match {
        case BenchmarkHeader(name) => 
          currentBenchmark = name
        case OrctimizerLine(n, force, resolve, future) => 
          out.write(s"$currentBenchmark\tOrctimizer\t$n\t$force\t$resolve\t$future\n")
        case PorcLine(n, force, closure, spawn, indir, dir) => 
          out.write(s"$currentBenchmark\tPorc\t$n\t$force\t$closure\t$spawn\t$indir\t$dir\n")
        case _ => ()
      }
    }
    
    out.close()
  }
}
