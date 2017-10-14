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

trait PorcEExperimentalCondition extends ExperimentalCondition {
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
  def toOrcArgs: Iterable[String] = Seq()
    
  def systemProperties: Map[String, Any] = Map()
  
  def toFilePrefix = orcFile.getName.stripSuffix(".orc") + "_" + productIterator.filterNot(v => v.asInstanceOf[AnyRef] eq orcFile).mkString("_")
  
  val orcFile: File
  
  def wrapperCmd: Seq[String] = Seq()
}

trait PorcEBenchmark {
  val workingDir = System.getProperty("user.dir", ".")
  val testRootDir = new File(workingDir).getParent
  val targetHost = Option(System.getProperty("orc.test.benchmarkingHost"))
  val remoteJavaHome = System.getProperty("orc.test.remoteJavaHome", "LocalInstalls/graalvm-0.28.2/jre")
  val targetBinariesDir = if (targetHost.isDefined) "orc-binaries" else testRootDir
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
      
  def makePathRemotable(p: String) = {
    targetBinariesDir + "/" + p.stripPrefix(testRootDir).stripPrefix("/")
  }
  
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

  val problemSize = System.getProperty("orc.test.benchmark.problemSize", "1").toInt
  val nRuns = System.getProperty("orc.test.benchmark.nRuns", "50").toInt
    
  /** This is a list of the all the CPU ids in the order they should be assigned.
    * 
    */
  val cpuIDList = (0 until 64).toSeq
  // The config should work for zemaitis: (0 until 64).toSeq
  // Configuration for lilith: Seq(0,4,2,6,1,5,3,7)
  // TODO: This needs to be configurable.
  
  def tasksetCommandPrefix(nCPUs: Int) = Seq("taskset", "--cpu-list", cpuIDList.take(nCPUs).mkString(","))

  protected def run(
    expCondition: PorcEExperimentalCondition,
    //jvmOptions: Seq[String],
    //orcOptions: Seq[String],
    //systemProperties: Map[String, String],
    runOutputDir: String) = {
    new File(runOutputDir).mkdirs()
    targetHost.foreach(RemoteCommand.mkdir(_, remoteOutputDir))

    val outFilenamePrefix = s"${expCondition.toFilePrefix}"
    //val systemPropOptions = for ((k, v) <- systemProperties) yield s"-D$k=$v"
    val outFile = s"${runOutputDir}/${outFilenamePrefix}_0.out"
    val errFile = s"${runOutputDir}/${outFilenamePrefix}_0.err"
    
    val outputDir = if (targetHost.isDefined) remoteOutputDir else runOutputDir
    
    val sshCommand = targetHost.toSeq.flatMap(host => Seq("ssh", host))
    
    val commandLine = expCondition.wrapperCmd ++
        Seq(javaCmd, "-XX:-UseJVMCIClassLoader", "-XX:-UseJVMCICompiler", "-cp", classPath, s"-Xbootclasspath/a:$bootPath") ++
        //jvmOptions ++
        //systemPropOptions ++
        Seq(
          s"-Dorc.test.benchmark.datadir=$targetBinariesDir/OrcTests/",
          s"-Dorc.test.benchmark.problemSize=${problemSize}",
          s"-Dorc.test.benchmark.nRuns=${nRuns}",
          s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_",
          "-Dorc.executionlog.filesuffix=_0",
          "-Dorc.config.dirs=config",
          s"-Dorc.executionlog.dir=${outputDir}",
          // Always trace compilation since it will be useful for determining where outlyers come from. 
          "-Dgraal.TraceTruffleCompilation=true", 
          "-Dgraal.TruffleBackgroundCompilation=false",
          s"-Dgraal.TruffleCompilerThreads=${Runtime.getRuntime.availableProcessors() / 2}") ++
        expCondition.toJvmArgs ++
        Seq("orc.Main",
          "--backend", "porc",
          "-O", "3") ++
        //orcOptions ++
        expCondition.toOrcArgs ++
        Seq(makePathRemotable(expCondition.orcFile.getPath))

    println(s"\n==== Starting $expCondition ====")
    println(s"With command line:\n${commandLine.mkString("\n  ")}")
    
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
  
  def runExperiment(experimentalConditions: Iterable[PorcEExperimentalCondition]) = {
    println(s"Running experiments:\n  ${experimentalConditions.mkString("\n  ")}")
    ExperimentalCondition.writeExperimentalConditionsTable(experimentalConditions)
    
    targetHost match {
      case Some(host) => 
        for (f <- filesToCopy) {
          println(s"Copying $f")
          RemoteCommand.mkdirAndRsync(s"$testRootDir/$f", host, s"$targetBinariesDir/$f")
        }
      case None => {
      }
    }
    
    val runOutputDir = System.getProperty("orc.executionlog.dir")
    for (expCondition <- experimentalConditions) {
      run(expCondition, runOutputDir)
    }
  }
}


