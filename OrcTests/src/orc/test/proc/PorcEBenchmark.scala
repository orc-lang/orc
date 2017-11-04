//
// DistribTestCase.scala -- Scala class DistribTestCase
// Project OrcTests
//
// Created by jthywiss on Jul 29, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.File
import java.io.FileOutputStream

import orc.test.util.OsCommand
import orc.test.util.ExperimentalCondition
import orc.test.util.FactorValue
import orc.test.util.RemoteCommand

// FIXME: This object contains a total mess. But I wanted to at least slightly abstract what I'm doing below. Needs to be refactored.
object ArthursBenchmarkEnv {
  val workingDir = System.getProperty("user.dir", ".")
  val testRootDir = new File(workingDir).getParent
  val targetHost = Option(System.getProperty("orc.test.benchmarkingHost"))
  val targetBinariesDir = if (targetHost.isDefined) "orc-binaries" else testRootDir
        
  def makePathRemotable(p: String) = {
    targetBinariesDir + "/" + p.stripPrefix(testRootDir).stripPrefix("/")
  }
      
  /** This is a list of the all the CPU ids in the order they should be assigned.
    * 
    */
  val cpuIDList = (0 until 64).toSeq
  // The config should work for zemaitis: (0 until 64).toSeq
  // Configuration for lilith: Seq(0,4,2,6,1,5,3,7)
  // TODO: This needs to be configurable.
  
  def tasksetCommandPrefix(nCPUs: Int) = Seq("taskset", "--cpu-list", cpuIDList.take(nCPUs).mkString(","))

  trait JVMRunner {
    def remoteJavaHome: String
    
    val problemSize = System.getProperty("orc.test.benchmark.problemSize", "1").toInt
    val nRuns = System.getProperty("orc.test.benchmark.nRuns", "50").toInt
  
    val remoteOutputDir = "runs"
    
    val filesToCopy = Seq(
        s"OrcScala/build",
        s"OrcScala/lib",
        s"PorcE/build/classes",
        s"OrcTests/build",
        s"OrcTests/test_data"
        )
    
    val classPathSeq = Seq(
        s"$targetBinariesDir/OrcScala/build/*",
        s"$targetBinariesDir/OrcScala/lib/*",
        s"$targetBinariesDir/PorcE/build/classes",
        s"$targetBinariesDir/OrcTests/build",
        )
    
    def quoteForShell(p: String) = {
      s"'$p'"
    }
  
    val bootPath = {
      val jh = if (targetHost.isDefined) remoteJavaHome else System.getProperty("java.home")
      s"${jh}/lib/truffle/truffle-api.jar"
    }
    val classPath = classPathSeq.mkString(":")
  
    val javaCmd = if (targetHost.isDefined) {
      remoteJavaHome + File.separator + "bin" + File.separator + "java";
    } else if (System.getProperty("os.name").startsWith("Win")) {
      System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
    } else {
      System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }
  
    protected def runJVM(
      expCondition: JVMExperimentalCondition,
      //jvmOptions: Seq[String],
      //orcOptions: Seq[String],
      //systemProperties: Map[String, String],
      runOutputDir: String,
      softTimeLimit: Double, 
      hardTimeLimit: Double) = {
      new File(runOutputDir).mkdirs()
      targetHost.foreach(RemoteCommand.mkdir(_, remoteOutputDir))
  
      val outFilenamePrefix = s"${expCondition.toFilePrefix}"
      //val systemPropOptions = for ((k, v) <- systemProperties) yield s"-D$k=$v"
      val outFile = s"${runOutputDir}/${outFilenamePrefix}_0.out"
      val errFile = s"${runOutputDir}/${outFilenamePrefix}_0.err"
      
      val outputDir = if (targetHost.isDefined) remoteOutputDir else runOutputDir
      
      val sshCommand = targetHost.toSeq.flatMap(host => Seq("ssh", host))
      
      val commandLine = expCondition.wrapperCmd ++
          Seq(javaCmd, "-cp", classPath, s"-Xbootclasspath/a:$bootPath") ++
          //jvmOptions ++
          //systemPropOptions ++
          Seq(
            s"-Dorc.test.benchmark.datadir=$targetBinariesDir/OrcTests/",
            s"-Dorc.test.benchmark.problemSize=${problemSize}",
            s"-Dorc.test.benchmark.nRuns=${nRuns}",
            s"-Dorc.test.benchmark.softTimeLimit=${softTimeLimit}",
            s"-Dorc.test.benchmark.hardTimeLimit=${hardTimeLimit}",
            s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_",
            "-Dorc.executionlog.filesuffix=_0",
            "-Dorc.config.dirs=config",
            s"-Dorc.executionlog.dir=${outputDir}") ++
          expCondition.toJvmArgs
  
      println(s"\n==== Starting $expCondition ====")
      println(s"With command line:\n${commandLine.mkString(" ")}")
      
      val maybeQuotedCommandLine = if (targetHost.isDefined) commandLine.map(quoteForShell) else commandLine
      
      OsCommand.getResultFrom(
        sshCommand ++ maybeQuotedCommandLine,
        directory = new File(workingDir),
        teeStdOutErr = true,
        stdoutTee = Seq(System.out, new FileOutputStream(outFile)),
        stderrTee = Seq(System.err, new FileOutputStream(errFile)))
      
      targetHost match {
        case Some(host) => 
          RemoteCommand.rsyncFromRemote(host, s"$remoteOutputDir/", runOutputDir)
          RemoteCommand.deleteDirectory(host, s"$remoteOutputDir")
        case None => {
        }
      }
    }
  }

  
  trait JVMExperimentalCondition extends ExperimentalCondition {
    override def toJvmArgs: Iterable[String] = {
      val factorProps = factorDescriptions.zipWithIndex.flatMap({ case (fd, i) => 
        val v = productElement(i).asInstanceOf[AnyRef] 
        val base = s"${FactorValue.factorPropertyPrefix}${fd.id}"
        Map(
            s"$base" -> v,
            s"$base.name" -> fd.name,
            s"$base.comments" -> fd.comments,
            s"$base.unit" -> fd.unit,
            )
      })
      val sysProps = systemProperties ++ factorProps
      val sysPropsAsArgs = for ((k, v) <- sysProps) yield s"-D$k=$v"
      sysPropsAsArgs
    }
      
    def systemProperties: Map[String, Any] = Map()
    
    def toFilePrefix: String
    
    def wrapperCmd: Seq[String] = Seq()
  }

  trait PorcEExperimentalCondition extends JVMExperimentalCondition {
    override def toJvmArgs = super.toJvmArgs ++
          Seq("-XX:-UseJVMCIClassLoader", "-XX:-UseJVMCICompiler",
            // Always trace compilation since it will be useful for determining where outlyers come from. 
            "-Dgraal.TraceTruffleCompilation=true", 
            "-Dgraal.TruffleBackgroundCompilation=false",
            ) ++
          Seq("orc.Main",
            "--backend", "porc",
            "-O", "3") ++
          toOrcArgs ++
          Seq(makePathRemotable(orcFile.getPath))

    def toOrcArgs: Iterable[String] = Seq()
    
    def toFilePrefix = orcFile.getName.stripSuffix(".orc") + "_" + productIterator.filterNot(v => v.asInstanceOf[AnyRef] eq orcFile).mkString("_")
    
    val orcFile: File
  }

  trait ScalaExperimentalCondition extends JVMExperimentalCondition {
    override def toJvmArgs = super.toJvmArgs ++
          Seq(benchmarkClass.getCanonicalName,
            "--benchmark")
          
    def toFilePrefix = benchmarkClass.getSimpleName.stripSuffix("$") + "_" + productIterator.filterNot(v => v.asInstanceOf[AnyRef] eq benchmarkClass).mkString("_")
    
    val benchmarkClass: Class[_]
  }
  
  trait CPUControlExperimentalCondition extends JVMExperimentalCondition {
    val nCPUs: Int
    
    override def toJvmArgs = Seq(s"-Dgraal.TruffleCompilerThreads=${nCPUs min 4}") ++ super.toJvmArgs 
    
    override def wrapperCmd = tasksetCommandPrefix(nCPUs)
  }
  
}

import ArthursBenchmarkEnv._
import java.time.Duration

trait PorcEBenchmark extends JVMRunner {
  lazy val remoteJavaHome = System.getProperty("orc.test.remoteJavaHome", "LocalInstalls/graalvm-0.28.2/jre")
  
  def hardTimeLimit: Double = softTimeLimit * 1.1
  def softTimeLimit: Double 
  
  def runExperiment(experimentalConditions: Iterable[JVMExperimentalCondition]) = {
    if (System.getProperty("orc.executionlog.dir") == null)
      ExperimentalCondition.writeExperimentalConditionsTable(experimentalConditions)
    else {
      val runOutputDir = System.getProperty("orc.executionlog.dir")
      println(s"orc.executionlog.dir is set. Assuming we are continuing an existing run. $runOutputDir/experimental-conditions.csv is not being rewritten. Make sure it's still correct.")
    }
    
    val runOutputDir = System.getProperty("orc.executionlog.dir")
    
    val selectedCond = Option(System.getProperty("orc.test.selectedTests")).map(_.split(","))
    val excludedCond = Option(System.getProperty("orc.test.excludedTests")).map(_.split(",")).getOrElse(Array())
    
    val filteredExperimentalConditions = experimentalConditions filter { cond =>
      val outputFile = s"$runOutputDir/${cond.toFilePrefix}_factor-values_0.csv"
      if (new File(outputFile).exists()) {
        println(s"Skipping benchmark because output already exists: $cond")
        false
      } else if (excludedCond contains cond.toFilePrefix) {
        println(s"Skipping benchmark because it is excluded: $cond")
        false
      } else if (selectedCond.isDefined && (selectedCond.get contains cond.toFilePrefix)) {
        true
      } else if (selectedCond.isDefined) {
        println(s"Skipping benchmark because it is NOT selected: $cond")
        false
      } else {
        true
      }
    }
    
    println(s"Running experiments:\n  ${filteredExperimentalConditions.mkString("\n  ")}")
    println(s"Soft maximum runtime: ${Duration.ofMillis((filteredExperimentalConditions.size * softTimeLimit * 1000).toLong)}")
    println(s"Hard maximum runtime: ${Duration.ofMillis((filteredExperimentalConditions.size * hardTimeLimit * 1000).toLong)}")
    
    targetHost match {
      case Some(host) => 
        for (f <- filesToCopy) {
          println(s"Copying $f")
          RemoteCommand.mkdirAndRsync(s"$testRootDir/$f", host, s"$targetBinariesDir/$f")
        }
      case None => {
      }
    }
    
    for (expCondition <- filteredExperimentalConditions) {
      runJVM(expCondition, runOutputDir, softTimeLimit, hardTimeLimit)
    }
  }
}

