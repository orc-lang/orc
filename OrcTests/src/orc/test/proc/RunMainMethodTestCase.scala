//
// RunMainMethodTestCase.scala -- Scala class RunMainMethodTestCase
// Project project_name
//
// Created by jthywiss on Oct 6, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import junit.framework.TestCase
import java.io.File
import orc.test.util.OsCommand
import java.io.FileOutputStream
import orc.test.util.TestRunNumber

/** JUnit test case for running a main class in its own JVM.
  *
  * @author jthywiss
  */
class RunMainMethodTestCase(
    val outFilenamePrefix: String,
    val testContext: Map[String, AnyRef],
    val testItem: Class[_],
    val mainArgs: String*)
  extends TestCase(testItem.getSimpleName) {

  /* Current state is messy: Refactor into a "run JVM" test case, a output conventions mix-in, and a run Orc mix-in, or somesuch. */

  def this(testContext: Map[String, AnyRef], testItem: Class[_], mainArgs: String*) = {
    this(testItem.getSimpleName, testContext, testItem, mainArgs: _*)
  }

  @throws(classOf[Throwable])
  override protected def runTest(): Unit = {
    println("\n==== Starting " + getName() + " ====")
    println("  " + (for ((k, v) <- testContext) yield s"$k=$v").mkString(", "))

    val runOutputDir = "runs/" + TestRunNumber.singletonNumber + "/raw-output"
    val testOutFile = new File(runOutputDir, outFilenamePrefix + ".out")
    val testErrFile = new File(runOutputDir, outFilenamePrefix + ".err")

    val orcVersion = orc.Main.versionProperties.getProperty("orc.version")
    val javaCmd = "java"
    val dOrcClassPath = s"../OrcScala/build/orc-${orcVersion}.jar:../OrcScala/lib/*:../PorcE/build/classes:../OrcTests/build" //DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get().mkString(File.pathSeparator)

    val javaRunCommand = Seq(
      javaCmd,
      "-cp",
      dOrcClassPath,
      "-Djava.util.logging.config.file=config/logging.properties",
      "-Dsun.io.serialization.extendedDebugInfo=true",
      "-Dorc.config.dirs=config",
      "-Dorc.executionlog.dir=" + runOutputDir,
      "-Dorc.executionlog.fileprefix=" + outFilenamePrefix + "_") ++ 
      //"-Dorc.executionlog.filesuffix=_0"
      (for ((k, v) <- testContext) yield s"-Dorc.test.$k=$v") ++
      Seq(testItem.getName()) ++ 
      mainArgs

    val result = OsCommand.getResultFrom(javaRunCommand, directory = new File("."), teeStdOutErr = true, stdoutTee = Seq(System.out, new FileOutputStream(testOutFile)), stderrTee = Seq(System.err, new FileOutputStream(testErrFile)))
    if (result.exitStatus != 0) {
      throw new AssertionError(s"${getName} failed: exitStatus=${result.exitStatus}")
    }

    println()
  }
}
