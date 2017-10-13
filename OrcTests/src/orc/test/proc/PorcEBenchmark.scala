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

trait PorcEExperimentalCondition extends ExperimentalCondition {
  override def toJvmArgs: Iterable[String] = {
    val sysProps = systemProperties ++ toMap.map({ case (k, v) => s"${FactorValue.factorPropertyPrefix}$k" -> v })
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
  var workingDir = System.getProperty("user.dir", ".")

  val bootPath = Option(System.getProperty("sun.boot.class.path"))

  val classPath = System.getProperty("java.class.path")

  val javaCmd = if (System.getProperty("os.name").startsWith("Win")) {
    System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
  } else {
    System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
  }

  val problemSize = System.getProperty("orc.test.benchmark.problemSize", "1").toInt
  val nRuns = System.getProperty("orc.test.benchmark.nRuns", "50").toInt

  protected def run(
    expCondition: PorcEExperimentalCondition,
    //jvmOptions: Seq[String],
    //orcOptions: Seq[String],
    //systemProperties: Map[String, String],
    runOutputDir: String) = {
    new File(runOutputDir).mkdirs()

    val outFilenamePrefix = s"${expCondition.toFilePrefix}"
    //val systemPropOptions = for ((k, v) <- systemProperties) yield s"-D$k=$v"
    val outFile = s"${runOutputDir}/${outFilenamePrefix}_0.out"
    val errFile = s"${runOutputDir}/${outFilenamePrefix}_0.err"
    
    val commandLine = expCondition.wrapperCmd ++
        Seq(javaCmd, "-cp", classPath) ++
        bootPath.map(p => s"-Xbootclasspath:$p") ++
        //jvmOptions ++
        //systemPropOptions ++
        expCondition.toJvmArgs ++
        Seq(
          s"-Dorc.test.benchmark.problemSize=${problemSize}",
          s"-Dorc.test.benchmark.nRuns=${nRuns}",
          s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_",
          "-Dorc.executionlog.filesuffix=_0",
          "-Dorc.config.dirs=config",
          s"-Dorc.executionlog.dir=${runOutputDir}",
          // Always trace compilation since it will be useful for determining where outlyers come from. 
          "-Dgraal.TraceTruffleCompilation=true", 
          "-Dgraal.TruffleBackgroundCompilation=false",
          s"-Dgraal.TruffleCompilerThreads=${Runtime.getRuntime.availableProcessors() / 2}", 
          "orc.Main",
          "--backend", "porc",
          "-O", "3",
          "-I", "src/orc/lib/includes") ++
          //orcOptions ++
          expCondition.toOrcArgs ++
          Seq(expCondition.orcFile.getPath)

    println(s"\n==== Starting $expCondition ====")
    println(s"==== With command line:\n${commandLine.mkString(" ")}")
    
    OsCommand.getResultFrom(
      commandLine,
      directory = new File(workingDir),
      teeStdOutErr = true,
      stdoutTee = Seq(System.out, new FileOutputStream(outFile)),
      stderrTee = Seq(System.err, new FileOutputStream(errFile)))
  }
  
  def runExperiment(experimentalConditions: Iterable[PorcEExperimentalCondition]) = {
    println(s"Running experiments:\n  ${experimentalConditions.mkString("\n  ")}")
    ExperimentalCondition.writeExperimentalConditionsTable(experimentalConditions)
    val runOutputDir = System.getProperty("orc.executionlog.dir")
    for (expCondition <- experimentalConditions) {
      run(expCondition, runOutputDir)
    }
  }
}


