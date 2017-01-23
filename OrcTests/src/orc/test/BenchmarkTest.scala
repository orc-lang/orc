//
// BenchmarkTest.scala -- Scala class/trait/object BenchmarkTest
// Project OrcTests
//
// Created by amp on Oct 27, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
import java.util.LinkedList
import java.util.Date
import scala.collection.JavaConversions._
import java.io.IOException
import java.text.SimpleDateFormat
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import orc.util.SynchronousThreadExec
import orc.TokenInterpreterBackend
import java.util.Properties
import java.io.FileInputStream

sealed trait BenchmarkSet
case object ScalaBenchmarkSet extends BenchmarkSet
case object OrcBenchmarkSet extends BenchmarkSet

case class BenchmarkConfig(
  benchmarkSet: BenchmarkSet,
  cpus: Seq[Int],
  backend: BackendType,
  optLevel: Int,
  output: File,
  tests: Int = Int.MaxValue,
  timeout: Long = 180L,
  nRuns: Int = 12,
  nDroppedRuns: Int = 4,
  outputCompileTime: Boolean = false,
  outputHeader: Boolean = true,
  outputStdDev: Boolean = true,
  nDroppedWarmups: Int = 1,
  timeoutLimit: Int = 2) {
  def name = s"$backend -O$optLevel on ${cpus.size} cpus"

  def asArguments: Seq[String] = Seq[String](
    "-o", output.getAbsolutePath(),
    "-p", cpus.mkString(","),
    "-B", backend.toString,
    "-O", optLevel.toString,
    "-#", tests.toString,
    "-t", timeout.toString,
    "-r", nRuns.toString,
    "-d", nDroppedRuns.toString,
    "-w", nDroppedWarmups.toString) ++
    (if (outputCompileTime) Seq("-C") else Seq()) ++
    (if (outputStdDev) Seq() else Seq("-s")) ++
    (if (outputHeader) Seq() else Seq("-H"))
}

/** @author amp
  */
object BenchmarkTest {
  object Logger extends orc.util.Logger("orc.test.benchmark")

  case class TimeData(compTime: Double, compStddev: Double, runTime: Double, runStddev: Double) {
    override def toString: String = {
      productIterator.mkString(",")
    }
    def toString(compTime: Boolean, stdDev: Boolean): String = {
      if (compTime)
        toString
      else if (stdDev)
        s"$runTime,$runStddev"
      else
        s"$runTime"
    }
  }

  private def makeBindings(opt: Int, backend: BackendType): orc.script.OrcBindings = {
    val b = new OrcBindings()
    b.optimizationLevel = opt
    b.backend = backend
    b
  }

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss")

  lazy val pid = try {
    new File("/proc/self").getCanonicalFile().getName().toInt
  } catch {
    case _: NumberFormatException => throw new UnsupportedOperationException("Getting the PID is only supported on Linux. Extend with additional platforms if you need to run it elsewhere.")
  }

  def main(args: Array[String]) {
    def processArgs(args: Seq[String], conf: BenchmarkConfig): BenchmarkConfig = args match {
      case "-p" +: arg +: rest =>
        val cpus = arg.split(",").toSeq.flatMap({ s =>
          if (s contains "-") {
            val vs = s.split("-")
            (vs(0).toInt to vs(1).toInt)
          } else {
            Seq(s.toInt)
          }
        })
        processArgs(rest, conf.copy(cpus = cpus))
      case "-c" +: arg +: rest =>
        processArgs(rest, conf.copy(cpus = 0 until arg.toInt))
      case "-S" +: arg +: rest =>
        val s = arg match {
          case "orc" => OrcBenchmarkSet
          case "scala" => ScalaBenchmarkSet
        }
        processArgs(rest, conf.copy(benchmarkSet = s))
      case "-#" +: arg +: rest =>
        processArgs(rest, conf.copy(tests = arg.toInt))
      case "-B" +: b +: rest =>
        processArgs(rest, conf.copy(backend = BackendType.fromString(b)))
      case "-O" +: o +: rest =>
        processArgs(rest, conf.copy(optLevel = o.toInt))
      case "-t" +: arg +: rest =>
        processArgs(rest, conf.copy(timeout = arg.toLong))
      case "-r" +: arg +: rest =>
        processArgs(rest, conf.copy(nRuns = arg.toInt))
      case "-d" +: arg +: rest =>
        processArgs(rest, conf.copy(nDroppedRuns = arg.toInt))
      case "-w" +: arg +: rest =>
        processArgs(rest, conf.copy(nDroppedWarmups = arg.toInt))
      case "-C" +: rest =>
        processArgs(rest, conf.copy(outputCompileTime = true))
      case "-s" +: rest =>
        processArgs(rest, conf.copy(outputStdDev = false))
      case "-H" +: rest =>
        processArgs(rest, conf.copy(outputHeader = false))
      case "-o" +: arg +: rest =>
        processArgs(rest, conf.copy(output = new File(arg).getAbsoluteFile()))
      case b if b.size == 0 =>
        conf
    }

    implicit val config = processArgs(args, BenchmarkConfig(OrcBenchmarkSet,
      0 to 128, TokenInterpreterBackend, 0,
      new File(s"benchmark-${dateFormatter.format(new Date())}.csv")))

    // generate a single row for the configuration.

    // script runs repeatedly for scaling measurements and other variation.

    val dataout = new OutputStreamWriter(new FileOutputStream(config.output, true))

    val testFilter = if (System.getenv("RUN_ONLY") != null) System.getenv("RUN_ONLY").split(",").toSet else Set[String]()
    def isSelectedTest(name: String): Boolean = {
      if (testFilter.size > 0) {
        testFilter contains name
      } else {
        true
      }
    }

    lazy val orcTests = {
      val path = new File("test_data/performance")
      val files = new LinkedList[File]()
      TestUtils.findOrcFiles(path, files);
      val alltests = for (file <- files.toSeq.reverse if isFileBenchmarked(file)) yield {
        val testname = if (file.toString().startsWith(path.getPath() + File.separator))
          file.toString().substring(path.getPath().length() + 1)
        else file.toString()

        (testname, file)
      }
      alltests.filter(p => isSelectedTest(p._1)).sortBy(_._1).take(config.tests)
    }

    lazy val scalaTests = {
      val scalabenchmarksConfigFile = new File("test_data/performance/scalabenchmarks.properties")
      val scalabenchmarksConfig = new Properties()
      scalabenchmarksConfig.load(new FileInputStream(scalabenchmarksConfigFile))
      val alltests = scalabenchmarksConfig.entrySet().map(e => {
        val clsName = e.getValue().asInstanceOf[String]
        val cls = Class.forName(clsName + "$")
        val moduleField = cls.getField("MODULE$")
        (e.getKey().asInstanceOf[String], () => moduleField.get(null).asInstanceOf[BenchmarkApplication])
      })
      alltests.toList.filter(p => isSelectedTest(p._1)).sortBy(_._1).take(config.tests)
    }

    {
      import scala.sys.process._
      s"taskset -a -c -p ${config.cpus.mkString(",")} $pid".!
    }

    def write(s: String) = {
      println("Data> " + s)
      Console.out.flush()
      dataout.write(s)
      dataout.flush()
    }

    config.benchmarkSet match {
      case OrcBenchmarkSet =>
        if (config.outputHeader) {
          write("Config," + orcTests.map(c => {
            val cname = c._1
            (if (config.outputCompileTime) s"$cname Comp. Avg,$cname Comp. Stdev," else "") +
              s"$cname Run Avg" + (if (config.outputStdDev) s",$cname Run Stdev" else "")
          }).mkString(",") + "\n")
        }
        write(s"${config.name}")
        for ((testname, file) <- orcTests) {
          val result = runTest(testname, file, makeBindings(config.optLevel, config.backend))
          write("," + result.toString(config.outputCompileTime, config.outputStdDev))
        }
        {
          val bindings = makeBindings(config.optLevel, config.backend)
          write(s",'${bindings.optimizationFlags}'")
        }
      case ScalaBenchmarkSet =>
        if (config.outputHeader) {
          write("Config," + scalaTests.map(c => {
            val cname = c._1
            (if (config.outputCompileTime) s"$cname Comp. Avg,$cname Comp. Stdev," else "") +
              s"$cname Run Avg" + (if (config.outputStdDev) s",$cname Run Stdev" else "")
          }).mkString(",") + "\n")
        }
        write(s"${config.name}")
        for ((testname, module) <- scalaTests) {
          val result = runTest(testname, module())
          write("," + result.toString(config.outputCompileTime, config.outputStdDev))
        }
    }

    write("\n")

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

  def runTest(testname: String, app: BenchmarkApplication)(implicit config: BenchmarkConfig): TimeData = {
    println(s"\n==== Benchmarking $testname ${app.getClass().getSimpleName()} ====")
    var timedout = 0

    val times = for (i <- 0 until config.nRuns) yield {
      try {
        SynchronousThreadExec(s"Benchmark $testname $i", config.timeout * 1000L, {
          print(s"$i:")
          System.gc()
          if (timedout >= config.timeoutLimit) {
            println(s" run SKIPPING DUE TO TOO MANY TIMEOUTS")
            config.timeout.toDouble
          } else {
            val (runTime: Double, _) = time {
              app.main(Array())
            }
            println(s" run $runTime")
            runTime
          }
        })
      } catch {
        case _: TimeoutException =>
          timedout += 1
          config.timeout
      }
    }

    // If we have enough drop the first (before sorting) to allow for JVM warm up.
    val runTimes = {
      val t = times.drop(config.nDroppedWarmups)
      Logger.info(s"Dropping leading measurement: ${times.take(config.nDroppedWarmups)} ::: ${t}")
      t
    }

    val (avgRunTime, sdRunTime) = medianAverage(runTimes)

    println(s">'$testname','native',,,,$avgRunTime,$sdRunTime,")

    TimeData(0, 0, avgRunTime, sdRunTime);
  }

  def runTest(testname: String, file: File, bindings: OrcBindings)(implicit config: BenchmarkConfig): TimeData = {
    println(s"\n==== Benchmarking $testname ${bindings.backend} -O${bindings.optimizationLevel} ====")
    try {
      var timedout = 0

      val times = for (i <- 0 until config.nRuns) yield {
        SynchronousThreadExec(s"Benchmark $testname $i", {
          print(s"$i:")
          System.gc()
          val (compTime, code) = time {
            compileCode(file, bindings)
          }
          System.gc()
          if (timedout >= config.timeoutLimit) {
            println(s" compile $compTime, run SKIPPING DUE TO TOO MANY TIMEOUTS")
            (compTime, config.timeout.toDouble)
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
      val (compTimes, runTimes) = {
        val t = times.drop(config.nDroppedWarmups).unzip
        Logger.info(s"Dropping leading 'warm-up' measurements: ${times.take(config.nDroppedWarmups).map(_._2)} ::: ${t._2}")
        t
      }

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
  def runCode(code: OrcScriptEngine[AnyRef]#OrcCompiledScript)(implicit config: BenchmarkConfig) = {
    OrcForTesting.run(code, config.timeout);
  }

  val compiledCodeCache = collection.mutable.Map[(File, OrcBindings), OrcScriptEngine[AnyRef]#OrcCompiledScript]()

  @throws(classOf[ClassNotFoundException])
  @throws(classOf[FileNotFoundException])
  @throws(classOf[OrcException])
  def compileCode(orcFile: File, bindings: OrcBindings)(implicit config: BenchmarkConfig): OrcScriptEngine[AnyRef]#OrcCompiledScript = {
    lazy val code = OrcForTesting.compile(orcFile.getPath(), bindings)
    if (!config.outputCompileTime) {
      // Use cache since we are not timing compile
      val key = (orcFile, bindings)
      if (compiledCodeCache.isDefinedAt(key)) {
        Logger.fine(s"Using cached compiled code for $orcFile.")
        OrcForTesting.importScript(orcFile.getPath(), bindings, compiledCodeCache(key))
      } else {
        Logger.info(s"Compiling code for $orcFile and inserting in the cache.")
        compiledCodeCache(key) = code
        code
      }
    } else {
      // We are timing compile so don't use cache
      code
    }
  }

  def time[A](f: => A): (Double, A) = {
    val start = System.nanoTime()
    val v = f;
    val stop = System.nanoTime()
    ((stop - start) / 1000000000.0, v)
  }

  def medianAverage(times: Seq[Double])(implicit config: BenchmarkConfig) = {
    assert(config.nDroppedRuns % 2 == 0)
    val toDrop = config.nDroppedRuns / 2
    val medians = if (times.size > config.nDroppedRuns) {
      val s = times.sorted
      val core = s.drop(toDrop).dropRight(toDrop)
      Logger.info(s"Dropping low and high measurement: ${s.take(toDrop)} ::: ${core} ::: ${s.takeRight(toDrop)}")
      core
    } else {
      times
    }

    val avg = medians.sum / medians.size
    val stddev = medians.map(x => (x - avg).abs).sum / medians.size
    Logger.fine(s"Averaging $medians: avg $avg, stddev $stddev")
    (avg, stddev)
  }
}
