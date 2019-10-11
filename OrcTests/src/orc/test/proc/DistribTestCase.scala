//
// DistribTestCase.scala -- Scala class DistribTestCase
// Project OrcTests
//
// Created by jthywiss on Jul 29, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.{ File, FileOutputStream, IOException }
import java.net.InetAddress
import java.nio.file.{ Files, Path, Paths, StandardOpenOption }
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import orc.error.compiletime.{ CompilationException, FeatureNotSupportedException }
import orc.script.OrcBindings
import orc.test.util.{ ExpectedOutput, ExperimentalCondition, OsCommand, RemoteCommand, TestRunNumber, TestUtils }
import orc.test.util.TestUtils.OrcTestCase
import orc.util.{ EventCounter, ShutdownHook, WikiCreoleTableWriter }

import junit.framework.TestSuite
import org.junit.Assume

/** JUnit test case for a distributed-Orc test.
  *
  * @author jthywiss
  */
class DistribTestCase(
    suitename: String,
    testname: String,
    file: Path,
    expecteds: ExpectedOutput,
    bindings: OrcBindings,
    val testContext: Map[String, AnyRef],
    val leaderSpec: DistribTestCase.DOrcRuntimePlacement,
    val followerSpecs: Seq[DistribTestCase.DOrcRuntimePlacement])
  extends OrcTestCase(suitename, testname, file, expecteds, bindings) {

  @throws[Throwable]
  override protected def runTest(): Unit = {
    println("\n==== Starting " + getName() + " ====")
    if (testContext != null) println("  " + (for ((k, v) <- testContext) yield s"$k=$v").mkString(", "))
    try {
      startFollowers()
      val exitStatus = runLeader()
      evaluateResult(exitStatus, ""); /* stdout not saved */
    } catch {
      case fnse: FeatureNotSupportedException => Assume.assumeNoException(fnse)
      case ce: CompilationException => throw new AssertionError(ce.getMessageAndDiagnostics())
    } finally {
      println()
      Thread.sleep(500 /*ms*/)
      stopFollowers()
      Thread.sleep(500 /*ms*/)
      println()
    }
  }

  def outFilenamePrefix = orcFile.getFileName.toString.stripSuffix(".orc")

  protected def runLeader(): Int = {
    val leaderOpts = DistribTestConfig.expanded.getIterableFor("leaderOpts").getOrElse(Seq())

    val jvmOptions = leaderSpec.jvmOptions ++ (if (testContext != null) for ((k, v) <- testContext) yield s"-Dorc.test.$k=$v" else Seq.empty)
    val leaderCommand = Seq(leaderSpec.javaCmd, "-cp", leaderSpec.classPath) ++ jvmOptions ++ Seq(s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_", "-Dorc.executionlog.filesuffix=_0", DistribTestConfig.expanded("leaderClass")) ++ leaderOpts.toSeq ++ Seq(s"--listen=${leaderSpec.hostname}:${leaderSpec.port}", s"--follower-count=${bindings.followerCount}", orcFile.toString)
    val leaderOutFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_0.out"
    val leaderErrFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_0.err"

    if (leaderSpec.isLocal) {
      OsCommand.runAndGetStatus(leaderCommand, workingDir = Paths.get(leaderSpec.workingDir), teeStdOutErr = true, stdoutTee = Seq(System.out, new FileOutputStream(leaderOutFile)), stderrTee = Seq(System.err, new FileOutputStream(leaderErrFile)))
    } else {
      RemoteCommand.runWithEcho(leaderSpec.hostname, leaderCommand, leaderSpec.workingDir, leaderOutFile, leaderErrFile)
    }
  }

  protected def startFollowers(): Unit = {
    for (followerNumber <- 1 to followerSpecs.size) {
      val followerSpec = followerSpecs(followerNumber - 1)
      val followerOpts = DistribTestConfig.expanded.getIterableFor("followerOpts").getOrElse(Seq())
      val jvmOptions = followerSpec.jvmOptions ++ (if (testContext != null) for ((k, v) <- testContext) yield s"-Dorc.test.$k=$v" else Seq.empty)
      val followerCommand = Seq(followerSpec.javaCmd, "-cp", followerSpec.classPath) ++ jvmOptions ++ Seq(s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_", s"-Dorc.executionlog.filesuffix=_$followerNumber", DistribTestConfig.expanded("followerClass")) ++ followerOpts.toSeq ++ Seq(s"--listen=${followerSpec.hostname}:${followerSpec.port}", followerNumber.toString, leaderSpec.hostname + ":" + leaderSpec.port)
      val followerOutFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_$followerNumber.out"
      val followerErrFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_$followerNumber.err"

      if (followerSpec.isLocal) {
        println(s"Launching follower $followerNumber on port ${followerSpec.port}")
        OsCommand.runNoWait(followerCommand, workingDir = Paths.get(followerSpec.workingDir), stdout = Paths.get(followerOutFile), stderr = Paths.get(followerErrFile))
      } else {
        println(s"Launching follower $followerNumber on ${followerSpec.hostname}:${followerSpec.port}")
        RemoteCommand.runNoEcho(followerSpec.hostname, followerCommand, followerSpec.workingDir, followerOutFile, followerErrFile)
      }
    }
  }

  protected def stopFollowers(): Unit = {
    for (followerNumber <- 1 to bindings.followerCount) {
      val followerSpec = followerSpecs(followerNumber - 1)

      if (followerSpec.isLocal) {
        val lsofResult = OsCommand.runAndGetResult(Seq("lsof", "-t", "-a", s"-i:${followerSpec.port}", "-sTCP:LISTEN"))
        if (lsofResult.exitStatus == 0) {
          println(s"Terminating follower $followerNumber on port ${followerSpec.port}")
          OsCommand.runAndGetResult(Seq("kill", lsofResult.stdout.stripLineEnd))
        }
      } else {
        val termResult = RemoteCommand.runAndGetResult(followerSpec.hostname, Seq("PID=`lsof -t -a -i:" + followerSpec.port + " -sTCP:LISTEN` && kill $PID"))
        if (termResult.exitStatus == 0) {
          println(s"Terminated follower $followerNumber on ${followerSpec.hostname}:${followerSpec.port}")
        }
      }
    }
  }

}

object DistribTestCase {

  case class DOrcRuntimePlacement(hostname: String, port: Int, isLocal: Boolean, workingDir: String, javaCmd: String, classPath: String, jvmOptions: Seq[String]) {}

  lazy val leaderHostname = DistribTestConfig.expanded("leaderHostname")
  lazy val leaderIsLocal = RemoteCommand.isLocalAddress(InetAddress.getByName(leaderHostname))
  lazy val remoteRunOutputDir = DistribTestConfig.expanded("runOutputDir").stripSuffix("/")

  def setUpTestSuite(): Unit = {

    /* Add extra variables to DistribTestConfig */
    DistribTestConfig.expanded.addVariable("currentJavaHome", System.getProperty("java.home"))
    //val currentJvmOpts = java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala
    //DistribTestConfig.expanded.addVariable("currentJvmOpts", currentJvmOpts)
    DistribTestConfig.expanded.addVariable("currentWorkingDir", System.getProperty("user.dir"))
    DistribTestConfig.expanded.addVariable("leaderHomeDir", if (leaderIsLocal) System.getProperty("user.home") else RemoteCommand.runAndGetResult(leaderHostname, Seq("pwd")).stdout.stripLineEnd)
    DistribTestConfig.expanded.addVariable("orcVersion", orc.Main.versionProperties.getProperty("orc.version"))
    DistribTestConfig.expanded.addVariable("testRunNumber", TestRunNumber.singletonNumber)

    /* Copy config dir to runOutputDir/../config */
    val localRunOutputDir = "../" + pathRelativeToTestRoot(remoteRunOutputDir)
    val orcConfigDir = "../" + pathRelativeToTestRoot(DistribTestConfig.expanded("orcConfigDir")).stripSuffix("/")
    Files.createDirectories(Paths.get(localRunOutputDir))
    OsCommand.checkExitValue(s"rsync of $orcConfigDir to $localRunOutputDir/../", OsCommand.runAndGetResult(Seq("rsync", "-rlpt", orcConfigDir, localRunOutputDir + "/../")))
    if (!leaderIsLocal) {
      copyFiles()
      RemoteCommand.mkdir(leaderHostname, remoteRunOutputDir)
      Runtime.getRuntime().addShutdownHook(DistribTestCopyBackThread)
    } else {
      Files.createDirectories(Paths.get(remoteRunOutputDir))
    }
  }

  type DistribTestCaseFactory = (String, String, Path, ExpectedOutput, OrcBindings, Map[String, AnyRef], DOrcRuntimePlacement, Seq[DOrcRuntimePlacement]) => DistribTestCase

  def buildSuite(programPaths: Array[Path]): TestSuite = {
    val testCaseFactory = (s, t, f, e, b, tc, ls, fs) => new DistribTestCase(s, t, f, e, b, tc, ls, fs)
    buildSuite(testCaseFactory, null, programPaths)
  }

  def buildSuite(testCaseFactory: DistribTestCaseFactory, testContext: Map[String, AnyRef], programPaths: Array[Path]): TestSuite = {
    if (testContext != null) {
      /* XXX */
      for ((key, value) <- testContext) DistribTestConfig.expanded.addVariable(key, value.toString())
    }
    val numRuntimes = DistribTestConfig.expanded.get("dOrcNumRuntimes").get.toInt
    val (leaderSpec, followerSpecs) = computeLeaderFollowerSpecs(numRuntimes)
    val options = new orc.Main.OrcCmdLineOptions()
    //options.parseCmdLine(DistribTestConfig.expanded.getIterableFor("leaderOpts").getOrElse(Seq()))
    options.backend = orc.BackendType.fromString("porc-distrib")
    options.showJavaStackTrace = true
    options.followerCount = followerSpecs.size
    TestUtils.buildSuite(classOf[DistribTest].getSimpleName(), (s, t, f, e, b) => testCaseFactory(s, t, f, e, b, testContext, leaderSpec, followerSpecs), options, programPaths: _*)
  }

  def computeLeaderFollowerSpecs(numRuntimes: Int): (DOrcRuntimePlacement, Seq[DOrcRuntimePlacement]) = {

    val javaCmd = DistribTestConfig.expanded("javaCmd")
    val dOrcClassPath = DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get.mkString(File.pathSeparator)
    val jvmOpts = DistribTestConfig.expanded.getIterableFor("jvmOpts").get.toSeq
    val leaderWorkingDir = DistribTestConfig.expanded("leaderWorkingDir")

    val followerHostnames = DistribTestConfig.expanded.getIndexed("followerHostname").get
    val dOrcPortBase = DistribTestConfig.expanded.get("dOrcPortBase").get.toInt
    val followerWorkingDir = DistribTestConfig.expanded("followerWorkingDir")

    val leaderSpec = DOrcRuntimePlacement(leaderHostname, dOrcPortBase, leaderIsLocal, leaderWorkingDir, javaCmd, dOrcClassPath, jvmOpts)

    val followerSpecs = for (followerNum <- 1 until numRuntimes) yield {
      val hostname = followerHostnames((followerNum - 1) % followerHostnames.size + 1)
      DOrcRuntimePlacement(hostname, dOrcPortBase + followerNum, RemoteCommand.isLocalAddress(InetAddress.getByName(hostname)), followerWorkingDir, javaCmd, dOrcClassPath, jvmOpts)
    }

    (leaderSpec, followerSpecs)
  }

  protected def pathRelativeToTestRoot(path: String): String = {
    Paths.get(DistribTestConfig.expanded("testRootDir")).normalize.relativize(Paths.get(path).normalize).toString
  }

  @throws[Exception]
  protected def copyFiles(): Unit = {

    print("Copying Orc test files to leader...")
    for (cpEntry <- DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get) {
      print(".")
      val localFilename = ".." + cpEntry.stripPrefix(DistribTestConfig.expanded("testRootDir")).stripSuffix("/*")
      val remoteFilename = cpEntry.stripSuffix("/*")
      RemoteCommand.mkdirAndRsync(localFilename, leaderHostname, remoteFilename)
    }
    print(".")
    RemoteCommand.mkdirAndRsync(s"config/logging.properties", leaderHostname, DistribTestConfig.expanded("loggingConfigFile"))
    print(".")
    val testDataDir = DistribTestConfig.expanded("testDataDir")
    RemoteCommand.mkdirAndRsync("../" + pathRelativeToTestRoot(testDataDir), leaderHostname, testDataDir)
    println("done")
  }

  private object DistribTestCopyBackThread extends ShutdownHook("DistribTestCopyBackThread") {
    override def run(): Unit = synchronized {
      val localRunOutputDir = "../" + pathRelativeToTestRoot(remoteRunOutputDir + "/")
      print(s"Copying run output from leader to $localRunOutputDir...")
      RemoteCommand.rsyncFromRemote(leaderHostname, remoteRunOutputDir + "/", localRunOutputDir)
      println("done")
    }
  }

  def writeReadme(testProcedureName: String, experimentalConditions: Traversable[ExperimentalCondition], testProgramNames: Traversable[String]): Unit = {
    val readmePath = Paths.get(System.getProperty("orc.executionlog.dir")).getParent.resolve("README.creole")
    val readmeWriter = Files.newBufferedWriter(readmePath, StandardOpenOption.CREATE_NEW)
    try {
      readmeWriter.append(composeReadme(testProcedureName, experimentalConditions, testProgramNames))
    } finally {
      readmeWriter.close()
    }
  }

  def composeReadme(testProcedureName: String, experimentalConditions: Traversable[ExperimentalCondition], testProgramNames: Traversable[String]): String = {
    val testRunNumber = TestRunNumber.singletonNumber
    val username = System.getProperty("user.name")
    val runDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    val gitDescribe = {
      try {
        OsCommand.runAndGetResult(Seq("git", "describe", "--dirty", "--tags", "--always")).stdout.stripLineEnd
      } catch {
        case _: IOException => null
      }
    }
    val gitLastCommit = {
      try {
        OsCommand.runAndGetResult(Seq("git", "log", "-1", "--format=%H")).stdout.stripLineEnd
      } catch {
        case _: IOException => null
      }
    }
    val gitStatus = {
      try {
        OsCommand.runAndGetResult(Seq("git", "status", "-s")).stdout.stripLineEnd
      } catch {
        case _: IOException => null
      }
    }
    val experimentalConditionsTable = new StringBuilder()
    val creoleWriter = new WikiCreoleTableWriter(experimentalConditionsTable.append(_))
    creoleWriter.writeHeader(experimentalConditions.head.factorDescriptions.map(_.toString))
    creoleWriter.writeRows(experimentalConditions)

    s"""= Run $testRunNumber =
       |
       |**Performed by:** $username
       |
       |**Date:** $runDate
       |
       |
       |== Item Under Test ==
       |
       |**Run test procedure class:** {{{$testProcedureName}}}
       |
       |
       |=== Programs Run ===
       |
       |""".stripMargin +
       testProgramNames.mkString("* {{{", "}}}\n* {{{", "}}}") +
    s"""
       |
       |
       |=== SCM Status ===
       |
       |**git describe:** {{{$gitDescribe}}}
       |
       |**git last commit hash:** {{{$gitLastCommit}}}
       |
       |**git status:**
       |{{{
       |""".stripMargin +
       gitStatus +
    s"""
       |}}}
       |
       |See the [[raw-output/envDescrip.json]] file for further details, including diffs to the SCM head.
       |
       |
       |== Parameter Values ==
       |
       |=== Experimental Conditions ===
       |
       |""".stripMargin +
       experimentalConditionsTable +
    s"""
       |
       |=== Configuration ===
       |
       |See the [[config]] directory for configuration parameter files.
       |
       |
       |== Raw Output ==
       |
       |Raw standard output & error files are in the [[raw-output]] directory, with files named using the schema //program//**_**//factors//**_**//n//**.**//ext//, where:
       |* //program// is the test program being run;
       |* //factors// are the experimental condition values for the test;
       |* //n// is 0 for the leader, or ≥ 1 for a follower;
       |* //ext// is {{{out}}} or {{{err}}}.
       |
       |Raw measurement data are also in the [[raw-output]] directory, with files named using the schema //program//**_**//factors//**_**//measurement//**_**//n//**.**//ext//, where:
       |* //program// is the test program being run;
       |* //factors// are the experimental condition values for the test;
       |* //measurement// is the kind of data (run times, event counts, profiler, etc.), omitted for output/error files,
       |* //n// is 0 for the leader, or ≥ 1 for a follower;
       |* //ext// is the file extension indicating the format.
       |
       |Detailed environment snapshots are also in the [[raw-output]] directory, with files named using the schema //program//**_**//factors//**_**envDescrip**_**//n//**.**json, where:
       |* //program// is the test program being run;
       |* //factors// are the experimental condition values for the test;
       |* //n// is 0 for the leader, or ≥ 1 for a follower;
       |
       |
       |== Analyses ==
       |
       |Analysis procedure results are in the {{{analysis-*}}} directories.
       |
       |In particular, elapsed time analysis is in the [[analysis-elapsedTime]] directory.""".stripMargin +
       (if (EventCounter.counterOn)
    s"""
       |
       |Event count analysis is in the [[analysis-eventCount]] directory.""".stripMargin else "") + 
    s"""
       |
       |
       |== Comments ==
       |
       |Commentary is in the [[notes]] file or directory.
       |""".stripMargin
  }

}
