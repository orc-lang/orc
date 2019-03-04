//
// DistribTestCase.scala -- Scala class DistribTestCase
// Project OrcTests
//
// Created by jthywiss on Jul 29, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc

import java.io.{ File, FileOutputStream }
import java.net.InetAddress
import java.nio.file.Paths

import orc.error.compiletime.{ CompilationException, FeatureNotSupportedException }
import orc.script.OrcBindings
import orc.test.util.{ ExpectedOutput, OsCommand, RemoteCommand, TestRunNumber, TestUtils }
import orc.test.util.TestUtils.OrcTestCase

import junit.framework.TestSuite
import org.junit.Assume

/** JUnit test case for a distributed-Orc test.
  *
  * @author jthywiss
  */
class DistribTestCase(
    suitename: String,
    testname: String,
    file: File,
    expecteds: ExpectedOutput,
    bindings: OrcBindings,
    val testContext: Map[String, AnyRef],
    val leaderSpec: DistribTestCase.DOrcRuntimePlacement,
    val followerSpecs: Seq[DistribTestCase.DOrcRuntimePlacement])
  extends OrcTestCase(suitename, testname, file, expecteds, bindings) {

  @throws[Throwable]
  override protected def runTest() {
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

  def outFilenamePrefix = orcFile.getName.stripSuffix(".orc")

  protected def runLeader() = {
    val leaderOpts = DistribTestConfig.expanded.getIterableFor("leaderOpts").getOrElse(Seq())

    val jvmOptions = leaderSpec.jvmOptions ++ (if (testContext != null) for ((k, v) <- testContext) yield s"-Dorc.test.$k=$v" else Seq.empty)
    val leaderOutFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_0.out"
    val leaderErrFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_0.err"

    if (leaderSpec.isLocal) {
      OsCommand.getStatusFrom(Seq(leaderSpec.javaCmd, "-cp", leaderSpec.classPath) ++ jvmOptions ++ Seq(s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_", "-Dorc.executionlog.filesuffix=_0", DistribTestConfig.expanded("leaderClass")) ++ leaderOpts.toSeq ++ Seq(s"--listen=${leaderSpec.hostname}:${leaderSpec.port}", s"--follower-count=${bindings.followerCount}", orcFile.getPath), directory = new File(leaderSpec.workingDir), teeStdOutErr = true, stdoutTee = Seq(System.out, new FileOutputStream(leaderOutFile)), stderrTee = Seq(System.err, new FileOutputStream(leaderErrFile)))
    } else {
      OsCommand.getStatusFrom(Seq("ssh", leaderSpec.hostname, s"cd '${leaderSpec.workingDir}'; { { '${leaderSpec.javaCmd}' -cp '${leaderSpec.classPath}' ${jvmOptions.map("'" + _ + "'").mkString(" ")} -Dorc.executionlog.fileprefix=${outFilenamePrefix}_ -Dorc.executionlog.filesuffix=_0 ${DistribTestConfig.expanded("leaderClass")} ${leaderOpts.mkString(" ")} --listen=${leaderSpec.hostname}:${leaderSpec.port} --follower-count=${bindings.followerCount} '$orcFile' | tee '$leaderOutFile'; exit $${PIPESTATUS[0]}; } 2>&1 1>&3 | tee '$leaderErrFile'; exit $${PIPESTATUS[0]}; } 3>&1 1>&2"), teeStdOutErr = true)
    }
  }

  protected def startFollowers() {
    for (followerNumber <- 1 to followerSpecs.size) {
      val followerSpec = followerSpecs(followerNumber - 1)
      val followerOpts = DistribTestConfig.expanded.getIterableFor("followerOpts").getOrElse(Seq())
      val jvmOptions = followerSpec.jvmOptions ++ (if (testContext != null) for ((k, v) <- testContext) yield s"-Dorc.test.$k=$v" else Seq.empty)
      val followerWorkingDir = followerSpec.workingDir
      val followerOutFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_$followerNumber.out"
      val followerErrFile = s"${DistribTestCase.remoteRunOutputDir}/${outFilenamePrefix}_$followerNumber.err"

      if (followerSpec.isLocal) {
        println(s"Launching follower $followerNumber on port ${followerSpec.port}")
        val command = Seq(followerSpec.javaCmd, "-cp", followerSpec.classPath) ++ jvmOptions ++ Seq(s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_", s"-Dorc.executionlog.filesuffix=_$followerNumber", DistribTestConfig.expanded("followerClass")) ++ followerOpts.toSeq ++ Seq(s"--listen=${followerSpec.hostname}:${followerSpec.port}", followerNumber.toString, leaderSpec.hostname + ":" + leaderSpec.port)
        OsCommand.runNoWait(command, directory = new File(followerWorkingDir), stdout = new File(followerOutFile), stderr = new File(followerErrFile))
      } else {
        println(s"Launching follower $followerNumber on ${followerSpec.hostname}:${followerSpec.port}")
        /* FIXME: Escape strings for shell */
        val command = Seq("ssh", followerSpec.hostname, s"cd '${followerSpec.workingDir}'; '${followerSpec.javaCmd}' -cp '${followerSpec.classPath}' ${jvmOptions.map("'" + _ + "'").mkString(" ")} -Dorc.executionlog.fileprefix=${outFilenamePrefix}_ -Dorc.executionlog.filesuffix=_$followerNumber ${DistribTestConfig.expanded("followerClass")} ${followerOpts.mkString(" ")} --listen=${followerSpec.hostname}:${followerSpec.port} $followerNumber ${leaderSpec.hostname}:${leaderSpec.port} >'$followerOutFile' 2>'$followerErrFile'")
        OsCommand.runNoWait(command)
      }
    }
  }

  protected def stopFollowers() {
    for (followerNumber <- 1 to bindings.followerCount) {
      val followerSpec = followerSpecs(followerNumber - 1)

      if (followerSpec.isLocal) {
        val lsofResult = OsCommand.getResultFrom(Seq("lsof", "-t", "-a", s"-i:${followerSpec.port}", "-sTCP:LISTEN"))
        if (lsofResult.exitStatus == 0) {
          println(s"Terminating follower $followerNumber on port ${followerSpec.port}")
          OsCommand.getResultFrom(Seq("kill", lsofResult.stdout.stripLineEnd))
        }
      } else {
        val termResult = OsCommand.getResultFrom(Seq("ssh", followerSpec.hostname, "PID=`lsof -t -a -i:" + followerSpec.port + " -sTCP:LISTEN` && kill $PID"))
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
    DistribTestConfig.expanded.addVariable("leaderHomeDir", if (leaderIsLocal) System.getProperty("user.home") else OsCommand.getResultFrom(Seq("ssh", leaderHostname, "pwd")).stdout.stripLineEnd)
    DistribTestConfig.expanded.addVariable("orcVersion", orc.Main.versionProperties.getProperty("orc.version"))
    DistribTestConfig.expanded.addVariable("testRunNumber", TestRunNumber.singletonNumber)

    /* Copy config dir to runOutputDir/../config */
    val localRunOutputDir = "../" + pathRelativeToTestRoot(remoteRunOutputDir)
    val orcConfigDir = "../" + pathRelativeToTestRoot(DistribTestConfig.expanded("orcConfigDir")).stripSuffix("/")
    new File(localRunOutputDir).mkdirs()
    OsCommand.checkExitValue(s"rsync of $orcConfigDir to $localRunOutputDir/../", OsCommand.getResultFrom(Seq("rsync", "-rlpt", orcConfigDir, localRunOutputDir + "/../")))
    if (!leaderIsLocal) {
      copyFiles()
      RemoteCommand.mkdir(leaderHostname, remoteRunOutputDir)
      Runtime.getRuntime().addShutdownHook(DistribTestCopyBackThread)
    } else {
      new File(remoteRunOutputDir).mkdirs()
    }
  }

  type DistribTestCaseFactory = (String, String, File, ExpectedOutput, OrcBindings, Map[String, AnyRef], DOrcRuntimePlacement, Seq[DOrcRuntimePlacement]) => DistribTestCase

  def buildSuite(programPaths: Array[File]): TestSuite = {
    val testCaseFactory = (s, t, f, e, b, tc, ls, fs) => new DistribTestCase(s, t, f, e, b, tc, ls, fs)
    buildSuite(testCaseFactory, null, programPaths)
  }

  def buildSuite(testCaseFactory: DistribTestCaseFactory, testContext: Map[String, AnyRef], programPaths: Array[File]): TestSuite = {
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

  //TODO
  //def runRemote(hostname: String, command: Seq[String], directory: File = null, stdin: String = "", stdout: File = null, stderr: File = null, charset: Charset = StandardCharsets.UTF_8) = {
  //  //... quote command properly ...
  //  OsCommand.run(Seq("ssh", hostname, s"cd $directory; command >stdout 2>stderr"), null, stdin, charset)
  //}

  //TODO
  //def getRemoteResultFrom(hostname: String, command: Seq[String], directory: File = null, stdin: String = "", charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: OutputStream = java.lang.System.out, stderrTee: OutputStream = java.lang.System.err) = {
  //  //... quote command properly ...
  //  OsCommand.getResultFrom(Seq("ssh", hostname, s"cd $directory; command >stdout 2>stderr"), null, stdin, charset, teeStdOutErr, stdoutTee, stderrTee)
  //}

  private object DistribTestCopyBackThread extends Thread("DistribTestCopyBackThread") {
    override def run = synchronized {
      val localRunOutputDir = "../" + pathRelativeToTestRoot(remoteRunOutputDir + "/")
      print(s"Copying run output from leader to $localRunOutputDir...")
      RemoteCommand.rsyncFromRemote(leaderHostname, remoteRunOutputDir + "/", localRunOutputDir)
      println("done")
    }
  }
}
