//
// BenchmarkApplication.scala -- Scala trait BenchmarkApplication
// Project OrcTests
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import java.nio.file.{ Files, Paths, StandardOpenOption }

import orc.lib.{ Benchmark, BenchmarkTimes }
import orc.test.util.{ FactorValue, TestEnvironmentDescription }
import orc.util.CsvWriter

import Util.time

trait BenchmarkApplication[T, R] {
  def setup(): T
  def benchmark(ctx: T): R
  def check(results: R): Boolean

  def size: Int
  val name: String

  override def toString() = s"BenchmarkApplication($name)"

  def runBenchmark(i: Int): BenchmarkTimes = {
    val ctx = setup()
    Benchmark.waitForCompilation()
    val (bt, r) = time(i, size)(benchmark(ctx))
    if (check(r)) {
      println(s"$name results are correct up to hash collision.")
    } else {
      throw new AssertionError(s"$name results are WRONG.")
    }
    bt
  }

  def setupOutput() = {
    if (System.getProperty("orc.executionlog.dir", "").isEmpty())
      throw new IllegalStateException("java system property orc.executionlog.dir must be set")
    val outDir = Paths.get(System.getProperty("orc.executionlog.dir"))
    if (Files.notExists(outDir)) {
      Files.createDirectories(outDir)
      println("Created output directory: " + outDir.toAbsolutePath)
    }
    TestEnvironmentDescription.dumpAtShutdown()
  }

  def buildOutputPathname(basename: String, extension: String): String = {
    val outDir = System.getProperty("orc.executionlog.dir", ".")
    val fileBasenamePrefix = System.getProperty("orc.executionlog.fileprefix", "")
    val fileBasenameSuffix = System.getProperty("orc.executionlog.filesuffix", "")
    outDir + "/" + fileBasenamePrefix + basename + fileBasenameSuffix + "." + extension
  }

  def writeCsvFileOverwrite(pathname: String, description: String, tableColumnTitles: Seq[String], rows: Seq[Seq[_]]) = {
    val outFile = Paths.get(pathname)
    val csvOsw = Files.newBufferedWriter(outFile, StandardOpenOption.CREATE_NEW)
    try {
      val csvWriter = new CsvWriter(csvOsw)
      csvWriter.writeHeader(tableColumnTitles)
      csvWriter.writeRowsOfTraversables(rows)
    } finally {
      csvOsw.close()
    }
    println(description + " written to " + outFile.toAbsolutePath)
  }

  def main(args: Array[String]): Unit = {
    if (args.size == 0) {
      val r = runBenchmark(0)
      println(r)
    } else if (args.size == 1 && args(0) == "--benchmark") {
      println("Benchmarking " + name + " (" + BenchmarkConfig.nRuns + " runs, problem size " + BenchmarkConfig.problemSize + ", O(" + size + ") work)")

      setupOutput()
      FactorValue.writeFactorValuesTableWithPropertyFactors(Seq(
        // Factor name, Value, Units, Comments
        ("Benchmark", name, "", "benchmarkName", ""),
        ("Number of Partitions", BenchmarkConfig.nPartitions, "", "nPartitions", "Number of parallel partitions or threads."),
        ("Problem Size", BenchmarkConfig.problemSize, "", "problemSize", "The parameter which controls the amount of work."),
        ("Work", size, "", "work", "An estimate of the amount of work this benchmark must."),
        ("Language", "Scala", "", "language", "")))

      val results = collection.mutable.Buffer[BenchmarkTimes]()

      val startTime = System.currentTimeMillis()
      def totalElapsedTime() = System.currentTimeMillis() - startTime
      BenchmarkConfig.startHardTimeLimit()

      try {
        for (i <- 0 until BenchmarkConfig.nRuns) {
          if (BenchmarkConfig.softTimeLimit > 0 && totalElapsedTime() > (BenchmarkConfig.softTimeLimit * 1000)) {
            throw new InterruptedException()
          }
          println("Start.")
          val bt = runBenchmark(i)
          println(s"End: $bt")
          results += bt
          writeCsvFileOverwrite(buildOutputPathname("benchmark-times", "csv"), "Benchmark times output file",
            Seq("Repetition number [rep]", "Elapsed time (s) [elapsedTime]", "Process CPU time (s) [cpuTime]",
              "Runtime compilation time (s) [rtCompTime]", "GC time (s) [gcTime]"),
            results.map(t => Seq[Any](t.iteration, t.runTime, t.cpuTime, t.compilationTime, t.gcTime)))
        }
      } catch {
        case _: InterruptedException =>
        // Exit normally, we hit soft limit.
      }
    } else if (args.size == 1) {
      val n = args(0).toInt
      for (i <- 0 until n) {
        val r = runBenchmark(i)
        println(r)
      }
    }
  }
}
