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
import scala.collection.JavaConverters._
import java.io.IOException
import java.text.SimpleDateFormat
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import orc.util.SynchronousThreadExec
import orc.TokenInterpreterBackend
import java.util.Properties
import java.io.FileInputStream
import java.lang.management.ManagementFactory

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
  outputCompileTime: Boolean = false,
  outputHeader: Boolean = true,
  outputCpuTime: Boolean = true,
  timeoutLimit: Int = 3) {

  def name = s"$backend -O$optLevel on ${cpus.size} cpus"
  def config = s"-O$optLevel"

  def oneRun = copy(nRuns = 1)

  def asArguments: Seq[String] = Seq[String](
    "-o", output.getAbsolutePath(),
    "-p", cpus.mkString(","),
    "-B", backend.toString,
    "-O", optLevel.toString,
    "-#", tests.toString,
    "-t", timeout.toString,
    "-r", nRuns.toString) ++
    (if (outputCompileTime) Seq("-C") else Seq()) ++
    (if (outputHeader) Seq() else Seq("-H"))
}

/** @author amp
  */
object BenchmarkTest {
  object Logger extends orc.util.Logger("orc.test.benchmark")

  lazy val versionProperties = {
    val p = new java.util.Properties()
    val vp = orc.Main.getClass().getResourceAsStream("version.properties")
    if (vp == null) throw new java.util.MissingResourceException("Unable to load version.properties resource", "/orc/version.properties", "")
    p.load(vp)
    p
  }
  lazy val scmRevision: String = versionProperties.getProperty("orc.scm-revision")
  lazy val orcVersion: String = versionProperties.getProperty("orc.version") + " rev. " + scmRevision + " (built " + versionProperties.getProperty("orc.build.date") + " " + versionProperties.getProperty("orc.build.user") + ") (JVM: " + System.getProperty("java.vm.version") + ")"

  case class RunData(implName: String, implRev: String, testName: String, testRev: String, config: String, nCpus: Int, compTime: Option[Double], runTime: Double, cpuTime: Double) {
    override def toString(): String = {
      s"$implName\t$implRev\t$testName\t$testRev\t$config\t$nCpus\t${compTime.getOrElse("NA")}\t$runTime\t$cpuTime"
    }
  }
  
  object RunData {
    val header = {
      "implName\timplRev\ttestName\ttestRev\tconfig\tnCpus\tcompTime\trunTime\tcpuTime"
    }
  }

  private def makeBindings(opt: Int, backend: BackendType): orc.script.OrcBindings = {
    val b = new OrcBindings()
    b.optimizationLevel = opt
    b.backend = backend
    b
  }
  
  object git {
    import scala.sys.process._
    
    def getLastRev(f: File): String = {
      val dirty = if(isChanged(f)) "-dirty" else ""
      Seq("git", "rev-list", "-1", "HEAD", f.toString()).!!<.stripLineEnd + dirty
    }
    
    def isChanged(f: File): Boolean = {
      Seq("git", "diff", "--name-only", f.toString()).!!<.size != 0 ||
      Seq("git", "diff", "--cached", "--name-only", f.toString()).!!<.size != 0
    }
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
        val s = arg.toLowerCase() match {
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
      case "-C" +: rest =>
        processArgs(rest, conf.copy(outputCompileTime = true))
      case "-H" +: rest =>
        processArgs(rest, conf.copy(outputHeader = false))
      case "-o" +: arg +: rest =>
        processArgs(rest, conf.copy(output = new File(arg).getAbsoluteFile()))
      case b if b.size == 0 =>
        conf
    }

    implicit val config = processArgs(args, BenchmarkConfig(OrcBenchmarkSet,
      0 until osmxbean.getAvailableProcessors, TokenInterpreterBackend, 0,
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
      val alltests = for (file <- files.asScala.toSeq.reverse if isFileBenchmarked(file)) yield {
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
      val alltests = scalabenchmarksConfig.entrySet().asScala.map(e => {
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
      println("Data> " + s + "\n")
      Console.out.flush()
      dataout.write(s + "\n")
      dataout.flush()
    }

    config.benchmarkSet match {
      case OrcBenchmarkSet =>
        if (config.outputHeader) {
          write(RunData.header)
        }
        println("Running each test once to warm up the VM")
        for ((testname, file) <- orcTests) {
          val result = runTest(testname, file, makeBindings(config.optLevel, config.backend))(config.oneRun)
          result foreach { d => write(d.toString()) }
        }
        for ((testname, file) <- orcTests) {
          val result = runTest(testname, file, makeBindings(config.optLevel, config.backend))
          result foreach { d => write(d.toString()) }
        }
      case ScalaBenchmarkSet =>
        if (config.outputHeader) {
          write(RunData.header)
        }
        println("Running each test once to warm up the VM")
        for ((testname, module) <- scalaTests) {
          val result = runTest(testname, module())(config.oneRun)
          result foreach { d => write(d.toString()) }
        }
        for ((testname, module) <- scalaTests) {
          val result = runTest(testname, module())
          result foreach { d => write(d.toString()) }
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

  def runTest(testname: String, app: BenchmarkApplication)(implicit config: BenchmarkConfig): Seq[RunData] = {
    println(s"\n==== Benchmarking $testname ${app.getClass().getSimpleName()} ====")
    var timedout = 0
    
    val sourceFile = {
      val sourceName = app.getClass().getCanonicalName().replaceAll("\\.","/").stripSuffix("$") + ".scala"
      val src = new File("src")
      new File(src, sourceName).getAbsoluteFile
    }
    
    def genData(runTime: Double, cpuTime: Double) = {
      RunData("scala", System.getProperty("java.vm.version"), testname, git.getLastRev(sourceFile), "", config.cpus.size, None, runTime, cpuTime)
    }
    
    for (i <- 0 until config.nRuns) yield {
      try {
        SynchronousThreadExec(s"Benchmark $testname $i", config.timeout * 1000L, {
          print(s"$i:")
          System.gc()
          if (timedout >= config.timeoutLimit) {
            println(s" run SKIPPING DUE TO TOO MANY TIMEOUTS")
            genData(config.timeout.toDouble, 0.0)
          } else {
            val (runTime: Double, cpuTime: Double, _) = time {
              app.main(Array())
            }
            println(s" run $runTime, cpu $cpuTime (${cpuTime / runTime} cores, ${cpuTime / runTime / osmxbean.getAvailableProcessors})")
            genData(runTime, cpuTime)
          }
        })
      } catch {
        case _: TimeoutException =>
          timedout += 1
          genData(config.timeout.toDouble, 0.0)
      }
    }
  }

  def runTest(testname: String, file: File, bindings: OrcBindings)(implicit config: BenchmarkConfig): Seq[RunData] = {
    println(s"\n==== Benchmarking $testname ${bindings.backend} -O${bindings.optimizationLevel} ====")
    try {
      var timedout = 0
    
      def genData(compTime: Option[Double], runTime: Double, cpuTime: Double) = {
        RunData(bindings.backend.toString(), orcVersion, testname, git.getLastRev(file), config.config, config.cpus.size, compTime, runTime, cpuTime)
      }

      for (i <- 0 until config.nRuns) yield {
        SynchronousThreadExec(s"Benchmark $testname $i", {
          print(s"$i:")
          System.gc()
          val (compTime, _, code) = time {
            compileCode(file, bindings)
          }
          System.gc()
          if (timedout >= config.timeoutLimit) {
            println(s" compile $compTime, run SKIPPING DUE TO TOO MANY TIMEOUTS")
            genData(Some(compTime), config.timeout.toDouble, 0.0)
          } else {
            val (runTime: Double, cpuTime, _) = time {
              try {
                runCode(code)
              } catch {
                case _: TimeoutException =>
                  timedout += 1
              }
            }
            println(s" compile $compTime, run $runTime, cpu $cpuTime (${cpuTime / runTime} cores, ${cpuTime / runTime / osmxbean.getAvailableProcessors})")
            genData(Some(compTime), runTime, cpuTime)
          }
        })
      }
    } catch {
      case ce: CompilationException =>
        throw new AssertionError(ce.getMessageAndDiagnostics())
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

  val osmxbean = ManagementFactory.getOperatingSystemMXBean() match {
    case v: com.sun.management.OperatingSystemMXBean => v
    case _ => throw new AssertionError("Benchmarking requires com.sun.management.OperatingSystemMXBean")
  }

  def time[A](f: => A): (Double, Double, A) = {
    val start = System.nanoTime()
    val startCpu = osmxbean.getProcessCpuTime
    val v = f;
    val stop = System.nanoTime()
    val stopCpu = osmxbean.getProcessCpuTime
    ((stop - start) / 1000000000.0, (stopCpu - startCpu) / 1000000000.0, v)
  }
}
