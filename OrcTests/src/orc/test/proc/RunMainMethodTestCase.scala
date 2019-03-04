//
// RunMainMethodTestCase.scala -- Scala class RunMainMethodTestCase
// Project OrcTests
//
// Created by jthywiss on Oct 6, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.{ File, FileOutputStream }
import java.net.InetAddress

import orc.test.util.{ OsCommand, RemoteCommand, TestRunNumber }

import junit.framework.TestCase

/** JUnit test case for running a main class in its own JVM.
  *
  * @author jthywiss
  */
class RunMainMethodTestCase(
    val outFilenamePrefix: String,
    val testContext: Map[String, AnyRef],
    val hostname: String,
    val workingDir: String,
    val testItem: Class[_],
    val mainArgs: String*)
  extends TestCase(testItem.getSimpleName) {

  /* Current state is messy: Refactor into a "run JVM" test case, a output conventions mix-in, and a run Orc mix-in, or somesuch. */

  def this(testContext: Map[String, AnyRef], hostname: String, workingDir: String, testItem: Class[_], mainArgs: String*) = {
    this(testItem.getSimpleName, testContext, hostname, workingDir, testItem, mainArgs: _*)
  }

  @throws[Throwable]
  override protected def runTest(): Unit = {
    println("\n==== Starting " + getName() + " ====")
    println("  " + (for ((k, v) <- testContext) yield s"$k=$v").mkString(", "))

    val runOutputDir = "runs/" + TestRunNumber.singletonNumber + "/raw-output"
    val testOutFile = new File(runOutputDir, outFilenamePrefix + ".out")
    val testErrFile = new File(runOutputDir, outFilenamePrefix + ".err")

    val orcVersion = orc.Main.versionProperties.getProperty("orc.version")
    val javaCmd = "java"
    val dOrcClassPath = s"../OrcScala/build/orc-${orcVersion}.jar:../OrcScala/lib/*:../PorcE/build/classes:../PorcE/lib/*:../OrcTests/build" //DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get().mkString(File.pathSeparator)

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

    val isLocal = RemoteCommand.isLocalAddress(InetAddress.getByName(hostname))
    val exitStatus =
      if (isLocal) {
        OsCommand.runAndGetStatus(javaRunCommand, workingDir = new File(workingDir), teeStdOutErr = true, stdoutTee = Seq(System.out, new FileOutputStream(testOutFile)), stderrTee = Seq(System.err, new FileOutputStream(testErrFile)))
      } else {
        OsCommand.runAndGetStatus(Seq("ssh", hostname, s"cd '${workingDir}'; { { ${javaRunCommand.mkString("'", "' '", "'")} | tee '$testOutFile'; exit $${PIPESTATUS[0]}; } 2>&1 1>&3 | tee 'testErrFile'; exit $${PIPESTATUS[0]}; } 3>&1 1>&2"), teeStdOutErr = true)
      }

    if (exitStatus != 0) {
      throw new AssertionError(s"${getName} failed: exitStatus=${exitStatus}")
    }

    println()
  }
}
