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

import java.io.{ File, FileOutputStream }
import java.net.{ InetAddress, InetSocketAddress, NetworkInterface, SocketException }

import scala.collection.JavaConverters.seqAsJavaListConverter

import orc.error.compiletime.{ CompilationException, FeatureNotSupportedException }
import orc.test.util.{ OsCommand, OsCommandResult, TestRunNumber, TestUtils }
import orc.test.util.TestUtils.OrcTestCase

import org.junit.Assume

/** JUnit test case for a distributed-Orc test.
  *
  * @author jthywiss
  */
class DistribTestCase extends OrcTestCase {

  @throws(classOf[Throwable])
  override def runTest() {
    val options = bindings.asInstanceOf[orc.Main.OrcCmdLineOptions]
    startFollowers()
    println("\n==== Starting " + orcFile + " ====")
    val leaderOpts = DistribTestConfig.expanded.getIterableFor("leaderOpts").getOrElse(Seq())
    val followerSockets = options.recognizedLongOpts("follower-sockets").getValue
    val outFilenamePrefix = orcFile.getName.stripSuffix(".orc")
    val runOutputDir = DistribTestConfig.expanded("runOutputDir")
    val leaderOutFile = new File(runOutputDir, s"${outFilenamePrefix}_0.out")
    val leaderErrFile = new File(runOutputDir, s"${outFilenamePrefix}_0.err")
    try {
      val actual = if (DistribTestCase.leaderSpec.isLocal) {
        //OrcForTesting.compileAndRun(orcFile.getPath(), 200 /*s*/, bindings)
        val result = OsCommand.getResultFrom(Seq(DistribTestCase.leaderSpec.javaCmd, "-cp", DistribTestCase.leaderSpec.classPath) ++ DistribTestCase.leaderSpec.jvmOptions ++ Seq(s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_", "-Dorc.executionlog.filesuffix=_0", DistribTestConfig.expanded("leaderClass")) ++ leaderOpts.toSeq ++ Seq(s"--follower-sockets=$followerSockets", orcFile.getPath), directory = new File(DistribTestCase.leaderSpec.workingDir), teeStdOutErr = true, stdoutTee = Seq(System.out, new FileOutputStream(leaderOutFile)), stderrTee = Seq(System.err, new FileOutputStream(leaderErrFile)))
        result.stdout
      } else {
        val result = OsCommand.getResultFrom(Seq("ssh", DistribTestCase.leaderSpec.hostname, s"cd '${DistribTestCase.leaderSpec.workingDir}'; { { '${DistribTestCase.leaderSpec.javaCmd}' -cp '${DistribTestCase.leaderSpec.classPath}' ${DistribTestCase.leaderSpec.jvmOptions.mkString(" ")} -Dorc.executionlog.fileprefix=${outFilenamePrefix}_ -Dorc.executionlog.filesuffix=_0 ${DistribTestConfig.expanded("leaderClass")} ${leaderOpts.mkString(" ")} --follower-sockets=$followerSockets '$orcFile' | tee '$leaderOutFile'; } 2>&1 1>&3 | tee '$leaderErrFile'; } 3>&1 1>&2"), teeStdOutErr = true)
        result.stdout
      }
      if (!expecteds.contains(actual)) {
        throw new AssertionError("Unexpected output:\n" + actual)
      }
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

  def startFollowers() {
    val outFilenamePrefix = orcFile.getName.stripSuffix(".orc")
    val runOutputDir = DistribTestConfig.expanded("runOutputDir")

    for (followerNumber <- 1 to DistribTestCase.followerSpecs.size) {
      val followerSpec = DistribTestCase.followerSpecs(followerNumber - 1)
      val followerOpts = DistribTestConfig.expanded.getIterableFor("followerOpts").getOrElse(Seq())
      val followerWorkingDir = new File(followerSpec.workingDir)
      val followerOutFile = new File(runOutputDir, s"${outFilenamePrefix}_$followerNumber.out")
      val followerErrFile = new File(runOutputDir, s"${outFilenamePrefix}_$followerNumber.err")

      if (followerSpec.isLocal) {
        println(s"Launching follower $followerNumber on port ${followerSpec.port}")
        val command = Seq(followerSpec.javaCmd, "-cp", followerSpec.classPath) ++ followerSpec.jvmOptions ++ Seq(s"-Dorc.executionlog.fileprefix=${outFilenamePrefix}_", s"-Dorc.executionlog.filesuffix=_$followerNumber", DistribTestConfig.expanded("followerClass")) ++ followerOpts.toSeq ++ Seq(followerNumber.toString, followerSpec.hostname+":"+followerSpec.port)
        OsCommand.runNoWait(command, directory = followerWorkingDir, stdout = followerOutFile, stderr = followerErrFile)
      } else {
        println(s"Launching follower $followerNumber on ${followerSpec.hostname}:${followerSpec.port}")
        /* FIXME: Escape strings for shell */
        val command = Seq("ssh", followerSpec.hostname, s"cd '${followerSpec.workingDir}'; '${followerSpec.javaCmd}' -cp '${followerSpec.classPath}' ${followerSpec.jvmOptions.mkString(" ")} -Dorc.executionlog.fileprefix=${outFilenamePrefix}_ -Dorc.executionlog.filesuffix=_$followerNumber ${DistribTestConfig.expanded("followerClass")} ${followerOpts.mkString(" ")} $followerNumber ${followerSpec.hostname}:${followerSpec.port} >'$followerOutFile' 2>'$followerErrFile'")
        OsCommand.runNoWait(command)
      }
      //TerminalWindow(commandSeq, s"Follower $followerNumber", 42, 132)
    }
  }

  def stopFollowers() {
    for (followerNumber <- 1 to bindings.followerSockets.size) {
      val followerSpec = DistribTestCase.followerSpecs(followerNumber - 1)

      if (followerSpec.isLocal) {
        val lsofResult = OsCommand.getResultFrom(Seq("lsof", "-t", "-a", s"-i:${followerSpec.port}", "-sTCP:LISTEN"))
        if (lsofResult.exitValue == 0) {
          println(s"Terminating follower $followerNumber on port ${followerSpec.port}")
          OsCommand.getResultFrom(Seq("kill", lsofResult.stdout.stripLineEnd))
        }
      } else {
        val termResult = OsCommand.getResultFrom(Seq("ssh", followerSpec.hostname, "PID=`lsof -t -a -i:"+followerSpec.port+" -sTCP:LISTEN` && kill $PID"))
        if (termResult.exitValue == 0) {
          println(s"Terminated follower $followerNumber on ${followerSpec.hostname}:${followerSpec.port}")
        }
      }
    }
  }

}

object DistribTestCase {

  def isLocalAddress(address: InetAddress): Boolean = {
    (address == null) || address.isLoopbackAddress || address.isAnyLocalAddress ||
      (try {
        NetworkInterface.getByInetAddress(address) != null
      } catch {
        case _: SocketException => false
      })
  }

  case class DOrcRuntimePlacement(hostname: String, port: Int, isLocal: Boolean, workingDir: String, javaCmd: String, classPath:String, jvmOptions: Seq[String]) { }

  def buildSuite() = {

    computeLeaderFollowerSpecs()

    val runOutputDir = DistribTestConfig.expanded("runOutputDir")
    if (!leaderSpec.isLocal) {
      copyFiles()
      checkExitValue(s"mkdir -p $runOutputDir on ${leaderSpec.hostname}", OsCommand.getResultFrom(Seq("ssh", leaderSpec.hostname, s"mkdir -p $runOutputDir")))
      Runtime.getRuntime().addShutdownHook(DistribTestCopyBackThread)
    } else {
      new File(runOutputDir).mkdirs()
    }

    val bindings = new orc.Main.OrcCmdLineOptions()
    bindings.backend = orc.BackendType.fromString("porc-distrib")
    bindings.followerSockets = (followerSpecs map { fs => new InetSocketAddress(fs.hostname, fs.port)}).toList.asJava
    bindings.showJavaStackTrace = true
    TestUtils.buildSuite(classOf[DistribTest].getSimpleName(), classOf[DistribTestCase], bindings, new File("test_data/functional_valid/distrib"))
  }

  var leaderSpec: DOrcRuntimePlacement = null
  var followerSpecs: Seq[DOrcRuntimePlacement] = null

  protected def computeLeaderFollowerSpecs(): Unit = {
    val currentJavaHome = System.getProperty("java.home")
    DistribTestConfig.expanded.addVariable("currentJavaHome", currentJavaHome)
    //val currentJvmOpts = java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala
    //DistribTestConfig.expanded.addVariable("currentJvmOpts", currentJvmOpts)
    val currentWorkingDir = System.getProperty("user.dir")
    DistribTestConfig.expanded.addVariable("currentWorkingDir", currentWorkingDir)

    val leaderHostname = DistribTestConfig.expanded("leaderHostname")
    val leaderIsLocal = isLocalAddress(InetAddress.getByName(leaderHostname))

    DistribTestConfig.expanded.addVariable("leaderHomeDir", if (leaderIsLocal) System.getProperty("user.home") else OsCommand.getResultFrom(Seq("ssh", leaderHostname, "pwd")).stdout.stripLineEnd)

    DistribTestConfig.expanded.addVariable("orcVersion", orc.Main.versionProperties.getProperty("orc.version"))

    DistribTestConfig.expanded.addVariable("testRunNumber", TestRunNumber.singletonNumber)

    val javaCmd = DistribTestConfig.expanded("javaCmd")
    val dOrcClassPath = DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get.mkString(File.pathSeparator)
    val jvmOpts = DistribTestConfig.expanded.getIterableFor("jvmOpts").get.toSeq
    val leaderWorkingDir = DistribTestConfig.expanded("leaderWorkingDir")

    val followerHostnames = DistribTestConfig.expanded.getIndexed("followerHostname").get
    val followerPorts = DistribTestConfig.expanded.getIndexed("followerPort").get.mapValues(_.toInt)
    val followerWorkingDir = DistribTestConfig.expanded("followerWorkingDir")
    assert (followerHostnames.keys == followerPorts.keys, "followerHostnames and followerPorts must cover same indicies")
    assert (followerHostnames.keys.last == followerHostnames.size, "followerHostnames and followerPorts must cover all indicies from 1 to the number of followers")

    leaderSpec = DOrcRuntimePlacement(leaderHostname, 0, leaderIsLocal, leaderWorkingDir, javaCmd, dOrcClassPath, jvmOpts)

    followerSpecs = followerHostnames.toSeq.map({case (followerNum, hostname) => DOrcRuntimePlacement(hostname, followerPorts(followerNum), isLocalAddress(InetAddress.getByName(hostname)), followerWorkingDir, javaCmd, dOrcClassPath, jvmOpts)})
  }

  @throws(classOf[Exception])
  protected def copyFiles(): Unit = {

    def mkdirAndRsync(localFilename: String, remoteHostname: String, remoteFilename: String): Unit = {
      val localFile = new File(localFilename)
      val localFileCanonicalName = localFile.getCanonicalPath + (if (localFile.isDirectory) "/" else "")
      val remoteDirPath = if (localFile.isDirectory) remoteFilename else new File(remoteFilename).getParent
      checkExitValue(s"mkdir -p $remoteDirPath on $remoteHostname", OsCommand.getResultFrom(Seq("ssh", remoteHostname, s"mkdir -p $remoteDirPath")))
      checkExitValue(s"rsync of $localFileCanonicalName to $remoteHostname:$remoteFilename", OsCommand.getResultFrom(Seq("rsync", "-rlpt", localFileCanonicalName, s"${remoteHostname}:${remoteFilename}")))
    }

    print("Copying Orc test files to leader...")
    for (cpEntry <- DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get) {
      print(".")
      val localFilename = ".." + cpEntry.stripPrefix(DistribTestConfig.expanded("testRootDir")).stripSuffix("/*")
      val remoteFilename = cpEntry.stripSuffix("/*")
      mkdirAndRsync(localFilename, leaderSpec.hostname, remoteFilename)
    }
    print(".")
    mkdirAndRsync(s"config/logging.properties", leaderSpec.hostname, DistribTestConfig.expanded("loggingConfigFile"))
    print(".")
    mkdirAndRsync("test_data/functional_valid/distrib/", leaderSpec.hostname, DistribTestConfig.expanded("dOrcTestDataDir"))
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


  @throws(classOf[CopyFilesException])
  protected def checkExitValue(description: String, result: OsCommandResult): Unit = {
    if (result.exitValue != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new CopyFilesException(s"${description} failed: exitValue=${result.exitValue}, stderr=${result.stderr}")
    }
  }

  private object DistribTestCopyBackThread extends Thread("DistribTestCopyBackThread") {
    override def run = synchronized {
      val remoteRunOutputDir = DistribTestConfig.expanded("runOutputDir").stripSuffix("/") + "/"
      val localRunOutputDir = ".." + remoteRunOutputDir.stripPrefix(DistribTestConfig.expanded("testRootDir"))
      print(s"Copying run output from leader to $localRunOutputDir...")
      new File(localRunOutputDir).mkdirs()
      checkExitValue(s"rsync of ${leaderSpec.hostname}:$remoteRunOutputDir to $localRunOutputDir", OsCommand.getResultFrom(Seq("rsync", "-rlpt", s"${leaderSpec.hostname}:$remoteRunOutputDir", localRunOutputDir)))
      println("done")
    }
  }
}


private class CopyFilesException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}
