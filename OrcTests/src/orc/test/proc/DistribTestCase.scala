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
import java.net.{ InetAddress, InetSocketAddress, NetworkInterface, SocketException }

import scala.collection.JavaConverters.{ asScalaBufferConverter, seqAsJavaListConverter }

import orc.error.compiletime.{ CompilationException, FeatureNotSupportedException }
import orc.test.util.{ OsCommand, OsCommandResult, TestUtils }
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
    try {
      val followerSockets = options.recognizedLongOpts("follower-sockets").getValue
      val actual = if (DistribTestCase.leaderIsLocal.get) {
        //OrcForTesting.compileAndRun(orcFile.getPath(), 200 /*s*/, bindings)
        val result = OsCommand.getResultFrom(Seq(s"${DistribTestCase.javaHome}/bin/java", "-cp", DistribTestCase.leaderSpec.classPath) ++ DistribTestCase.leaderSpec.jvmOptions ++ Seq("orc.Main", "--backend=porc-distrib", s"--follower-sockets=$followerSockets", "--java-stack-trace", orcFile.getPath), directory = new File(DistribTestCase.leaderSpec.workingDir), teeStdOutErr = true)
        result.stdout
      } else {
        val result = OsCommand.getResultFrom(Seq("ssh", DistribTestCase.leaderSpec.hostname, s"cd '${DistribTestCase.leaderSpec.workingDir}' ; '${DistribTestCase.javaHome}/bin/java' -cp '${DistribTestCase.leaderSpec.classPath}' ${DistribTestCase.leaderSpec.jvmOptions.mkString(" ")} orc.Main -O 3 --backend=porc-distrib --follower-sockets=$followerSockets --java-stack-trace $orcFile"), teeStdOutErr = true)
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

    for (followerNumber <- 1 to DistribTestCase.followerSpecs.size) {
      val followerSpec = DistribTestCase.followerSpecs(followerNumber - 1)
      val address = bindings.followerSockets.get(followerNumber - 1).getAddress

      val commandSeq = if (DistribTestCase.isLocalAddress(address)) {
        println(s"Launching follower $followerNumber on port ${followerSpec.port}")
        Seq(
            Seq("cd", followerSpec.workingDir),
            Seq(s"${DistribTestCase.javaHome}/bin/java", "-cp", followerSpec.classPath) ++ followerSpec.jvmOptions ++ Seq("orc.run.porce.distrib.FollowerRuntime", followerNumber.toString, followerSpec.hostname+":"+followerSpec.port),
        )
      } else {
        val sshAddress = bindings.followerSockets.get(followerNumber - 1).getHostString
        println(s"Launching follower $followerNumber on $sshAddress:${followerSpec.port}")
        /* FIXME: Escape strings for shell */
        Seq(Seq("ssh", sshAddress, s"cd '${followerSpec.workingDir}' ; '${DistribTestCase.javaHome}/bin/java' -cp '${followerSpec.classPath}' ${followerSpec.jvmOptions.mkString(" ")} orc.run.porce.distrib.FollowerRuntime $followerNumber ${followerSpec.hostname}:${followerSpec.port}"))
      }
      OsCommand.newTerminalWindowWith(commandSeq, s"Follower $followerNumber", 42, 132)
    }
  }

  def stopFollowers() {
    for (followerNumber <- 1 to bindings.followerSockets.size) {
      val address = bindings.followerSockets.get(followerNumber - 1).getAddress
      val port = bindings.followerSockets.get(followerNumber - 1).getPort
      if (DistribTestCase.isLocalAddress(address)) {
        val lsofResult = OsCommand.getResultFrom(Seq("lsof", "-t", "-a", s"-i:$port", "-sTCP:LISTEN"))
        if (lsofResult.exitValue == 0) {
          println(s"Terminating follower $followerNumber on port $port")
          OsCommand.getResultFrom(Seq("kill", lsofResult.stdout.stripLineEnd))
        }
      } else {
        val sshAddress = bindings.followerSockets.get(followerNumber - 1).getHostString
        val termResult = OsCommand.getResultFrom(Seq("ssh", sshAddress, "PID=`lsof -t -a -i:"+port+" -sTCP:LISTEN` && kill $PID"))
        if (termResult.exitValue == 0) {
          println(s"Terminated follower $followerNumber on $sshAddress:$port")
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

  case class DOrcRuntimePlacement(hostname: String, port: Int, workingDir: String, classPath:String, jvmOptions: Seq[String]) { }

  def buildSuite() = {

    computeLeaderFollowerSpecs()

    if (!leaderIsLocal.get) {
      copyFiles()
    }
    val bindings = new orc.Main.OrcCmdLineOptions()
    bindings.backend = orc.BackendType.fromString("porc-distrib")
    bindings.followerSockets = (followerSpecs map { fs => new InetSocketAddress(fs.hostname, fs.port)}).toList.asJava
    bindings.showJavaStackTrace = true
    TestUtils.buildSuite(classOf[DistribTest].getSimpleName(), classOf[DistribTestCase], bindings, new File("test_data/distrib"))
  }

  var leaderIsLocal: Option[Boolean] = None
  var javaHome: String = null
  var dOrcClassPath: String = null
  var leaderSpec: DOrcRuntimePlacement = null
  var followerSpecs: Seq[DOrcRuntimePlacement] = null

  protected def computeLeaderFollowerSpecs(): Unit = {
    val currentJavaHome = System.getProperty("java.home")
    DistribTestConfig.expanded.addMacro("currentJavaHome", currentJavaHome)
    val currentWorkingDir = System.getProperty("user.dir")
    DistribTestConfig.expanded.addMacro("currentWorkingDir", currentWorkingDir)
  
    val leaderHostname = DistribTestConfig.expanded.getOrElse("leaderHostname", "localhost")
    leaderIsLocal = Some(isLocalAddress(InetAddress.getByName(leaderHostname)))
  
    DistribTestConfig.expanded.addMacro("leaderHomeDir", if (leaderIsLocal.get) System.getProperty("user.home") else OsCommand.getResultFrom(Seq("ssh", leaderHostname, "pwd")).stdout.stripLineEnd)
    
    DistribTestConfig.expanded.addMacro("orcVersion", orc.Main.versionProperties.getProperty("orc.version"))
  
    javaHome = DistribTestConfig.expanded.getOrElse("javaHome", currentJavaHome)
    dOrcClassPath = DistribTestConfig.expanded.getIterableFor("dOrcClassPath").map(_.mkString(File.pathSeparator)).getOrElse(System.getProperty("java.class.path"))
    val jvmOpts = DistribTestConfig.expanded.getIterableFor("jvmOpts").getOrElse(java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala).toSeq
  
    val leaderWorkingDir = DistribTestConfig.expanded.getOrElse("leaderWorkingDir", currentWorkingDir)
  
    val followerHostnames = DistribTestConfig.expanded.getIndexed("followerHostname").get
    val followerPorts = DistribTestConfig.expanded.getIndexed("followerPort").get.mapValues(_.toInt)
    val followerWorkingDir = DistribTestConfig.expanded("followerWorkingDir")
    assert (followerHostnames.keys == followerPorts.keys, "followerHostnames and followerPorts must cover same indicies")
    assert (followerHostnames.keys.last == followerHostnames.size, "followerHostnames and followerPorts must cover all indicies from 1 to the number of followers")
  
    leaderSpec = DOrcRuntimePlacement(leaderHostname, 0, leaderWorkingDir, dOrcClassPath, jvmOpts)
  
    followerSpecs = followerHostnames.toSeq.map({case (followerNum, hostname) => DOrcRuntimePlacement(hostname, followerPorts(followerNum), followerWorkingDir, dOrcClassPath, jvmOpts)})
  }

  @throws(classOf[Exception])
  protected def copyFiles(): Unit = {
    def checkExitValue(description: String, result: OsCommandResult): Unit = {
      if (result.exitValue != 0) {
        print(result.stdout)
        Console.err.print(result.stderr)
        throw new Exception(s"${description} failed: exitValue=${result.exitValue}, stderr=${result.stderr}")
      }
    }

    def mkdirAndRsync(localFilename: String, remoteHostname: String, remoteFilename: String): Unit = {
      val localFile = new File(localFilename)
      val localFileCanonicalName = localFile.getCanonicalPath + (if (localFile.isDirectory) "/" else "")
      val remoteDirPath = if (localFile.isDirectory) remoteFilename else new File(remoteFilename).getParent
      checkExitValue(s"mkdir -p $remoteDirPath on $remoteHostname", OsCommand.getResultFrom(Seq("ssh", remoteHostname, s"mkdir -p $remoteDirPath")))
      checkExitValue(s"rsync of $localFileCanonicalName to $remoteHostname:$remoteFilename", OsCommand.getResultFrom(Seq("rsync", "-rlpt", localFileCanonicalName, s"${remoteHostname}:${remoteFilename}")))
    }

    print("Copying Orc test files to leader...")
    for (cpEntry <- DistribTestConfig.expanded.getIterableFor("dOrcClassPath").get) {
      //print(".")
      val localFilename = ".." + cpEntry.stripPrefix(DistribTestConfig.expanded("testRootDir")).stripSuffix("/*")
      val remoteFilename = cpEntry.stripSuffix("/*")
      mkdirAndRsync(localFilename, leaderSpec.hostname, remoteFilename)
    }
    //print(".")
    mkdirAndRsync(s"config/logging.properties", leaderSpec.hostname, DistribTestConfig.expanded("loggingConfigFile"))
    //print(".")
    mkdirAndRsync("test_data/distrib/", leaderSpec.hostname, DistribTestConfig.expanded("dOrcTestDataDir"))
    println("done")
  }

}
