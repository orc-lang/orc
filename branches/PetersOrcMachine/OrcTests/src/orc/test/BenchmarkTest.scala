//
// BenchmarkTest.scala -- Scala class/trait/object BenchmarkTest
// Project OrcTests
//
// $Id$
//
// Created by amp on Oct 27, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.test

import orc.test.TestUtils.OrcTestCase
import orc.error.compiletime.CompilationException
import orc.error.OrcException
import java.util.concurrent.TimeoutException
import java.io.FileNotFoundException
import orc.script.OrcScriptEngine
import orc.script.OrcBindings
import java.io.File
import orc.BackendType
import java.util.ArrayList
import java.util.Date
import scala.collection.JavaConversions._
import java.io.IOException
import java.text.SimpleDateFormat
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import orc.util.SynchronousThreadExec
import orc.PorcInterpreterBackend
import orc.TokenInterpreterBackend

/** @author amp
  */
object BenchmarkTest {
  val BENCHMARKING_TIMEOUT = 180L
  val N_RUNS = 15
  val N_DROP = 6
  val INCLUDE_COMPTIME = false

  case class TimeData(compTime: Double, compStddev: Double, runTime: Double, runStddev: Double) {
    override def toString: String = {
      productIterator.mkString(",")
    }
    def toString(compTime: Boolean): String = {
      if (compTime)
        toString
      else
        s"$runTime,$runStddev"
    }
  }

  private def makeBindings(opt: Int, backend: BackendType): orc.script.OrcBindings = {
    val b = new OrcBindings()
    b.optimizationLevel = opt
    b.backend = backend
    b
  }

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss")

  def main(args: Array[String]) {
    val datafile = new File(s"benchmark-${dateFormatter.format(new Date())}.csv")
    val dataout = new OutputStreamWriter(new FileOutputStream(datafile))

    val path = new File("test_data/performance")
    val files = new ArrayList[File]()
    TestUtils.findOrcFiles(path, files);
    val tests = for (file <- files.toSeq if isFileBenchmarked(file)) yield {
      val testname = if (file.toString().startsWith(path.getPath() + File.separator))
        file.toString().substring(path.getPath().length() + 1)
      else file.toString();

      (testname, file)
    }

    val configs = for (opt <- List(0, 1, 3); backend <- List(TokenInterpreterBackend, PorcInterpreterBackend))
      yield makeBindings(opt, backend)

    dataout.write("Test," + configs.map(c => {
      val cname = s"${c.backend.toString.head}-O${c.optimizationLevel}"
      (if (INCLUDE_COMPTIME) s"$cname Comp. Avg,$cname Comp. Std," else "") +
        s"$cname Run Avg,$cname Run Std"
    }).mkString(",") + "\n")
    dataout.flush()

    for ((testname, file) <- tests) yield {
      val results = for (bindings <- configs) yield {
        runTest(testname, file, bindings)
      }
      println(s"$testname," + results.map(_.toString(INCLUDE_COMPTIME)).mkString(",") + "\n")
      dataout.write(s"$testname," + results.map(_.toString(INCLUDE_COMPTIME)).mkString(",") + "\n")
      dataout.flush()
    }

    dataout.close()
  }

  def isFileBenchmarked(file: File) = {
    val expecteds = try {
      new ExpectedOutput(file);
    } catch {
      case e: IOException =>
        throw new AssertionError(e)
    }
    expecteds.shouldBenchmark()
  }

  def runTest(testname: String, file: File, bindings: OrcBindings) = {
    println(s"\n==== Benchmarking $testname ${bindings.backend} -O${bindings.optimizationLevel} ====");
    try {
      var timedout = 0

      val times = for (i <- 0 until N_RUNS) yield {
        SynchronousThreadExec("Benchmark $testname $i", {
          print(s"$i:")
          val (compTime, code) = time {
            compileCode(file, bindings)
          }
          if (timedout > N_RUNS / 3) {
            println(s" compile $compTime, run SKIPPING DUE TO TOO MANY TIMEOUTS")
            (compTime, BENCHMARKING_TIMEOUT.toDouble)
          } else {
            val (runTime: Double, _) = time {
              try {
                runCode(code)
              } catch {
                case _: TimeoutException =>
                  timedout += 1
              }
            }
            println(s" compile $compTime, run $runTime")
            (compTime, runTime)
          }
        })
      }

      // If we have enough drop the first (before sorting) to allow for JVM warm up.
      val (compTimes, runTimes) = if (times.size > 5) times.tail.unzip else times.unzip

      val (avgCompTime, sdCompTime) = medianAverage(compTimes)
      val (avgRunTime, sdRunTime) = medianAverage(runTimes)

      println(s">'$testname','${bindings.backend}',${bindings.optimizationLevel},$avgCompTime,$sdCompTime,$avgRunTime,$sdRunTime,'${bindings.optimizationFlags}'")

      TimeData(avgCompTime, sdCompTime, avgRunTime, sdRunTime);
    } catch {
      case ce: CompilationException =>
        throw new AssertionError(ce.getMessageAndDiagnostics());
    }
  }

  @throws(classOf[OrcException])
  @throws(classOf[TimeoutException])
  def runCode(code: OrcScriptEngine[AnyRef]#OrcCompiledScript) = {
    OrcForTesting.run(code, BENCHMARKING_TIMEOUT);
  }

  @throws(classOf[ClassNotFoundException])
  @throws(classOf[FileNotFoundException])
  @throws(classOf[OrcException])
  def compileCode(orcFile: File, bindings: OrcBindings): OrcScriptEngine[AnyRef]#OrcCompiledScript = {
    OrcForTesting.compile(orcFile.getPath(), bindings);
  }

  def time[A](f: => A): (Double, A) = {
    val start = System.nanoTime()
    val v = f;
    val stop = System.nanoTime()
    ((stop - start) / 1000000000.0, v)
  }

  def medianAverage(times: Seq[Double]) = {
    assert(N_DROP % 2 == 0)
    val medians = if (times.size > N_DROP)
      times.sorted.drop(N_DROP / 2).dropRight(N_DROP / 2)
    else
      times

    val avg = medians.sum / medians.size
    val stddev = medians.map(x => (x - avg).abs).sum / medians.size
    (avg, stddev)
  }
}