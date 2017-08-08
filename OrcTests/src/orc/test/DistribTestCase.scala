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

package orc.test

import java.io.File
import java.net.{ InetAddress, InetSocketAddress, NetworkInterface, SocketException }

import scala.collection.JavaConverters.seqAsJavaListConverter

import orc.DistributedBackendType
import orc.error.compiletime.{ CompilationException, FeatureNotSupportedException }
import orc.test.TestUtils.OrcTestCase

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
      val actual = if (isLocalAddress(InetAddress.getByName(DistribTestCase.leaderSpec.hostname))) {
        //OrcForTesting.compileAndRun(orcFile.getPath(), 200 /*s*/, bindings)
        val result = OsCommand.getResultFrom(Seq("java", "-cp", DistribTestCase.leaderSpec.classPath) ++ DistribTestCase.leaderSpec.jvmOptions ++ Seq("orc.Main", "--backend=distrib", s"--follower-sockets=$followerSockets", "--java-stack-trace", orcFile.getPath), directory = new File(DistribTestCase.leaderSpec.workingDir), teeStdOutErr = true)
        result.stdout
      } else {
        val result = OsCommand.getResultFrom(Seq("ssh", DistribTestCase.leaderSpec.hostname, s"cd '${DistribTestCase.leaderSpec.workingDir}' ; java -cp '${DistribTestCase.leaderSpec.classPath}' ${DistribTestCase.leaderSpec.jvmOptions.mkString(" ")} orc.Main --backend=distrib --follower-sockets=$followerSockets --java-stack-trace $orcFile"), teeStdOutErr = true)
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

  def isLocalAddress(address: InetAddress) = {
    address == null || address.isLoopbackAddress || address.isAnyLocalAddress ||
      (try {
        val x = NetworkInterface.getByInetAddress(address) != null
        x
      } catch {
        case _: SocketException => false
      })
  }

  def startFollowers() {

    for (followerNumber <- 1 to DistribTestCase.followerSpecs.size) {
      val followerSpec = DistribTestCase.followerSpecs(followerNumber - 1)
      val address = bindings.followerSockets.get(followerNumber - 1).getAddress
      
      val commandSeq = if (isLocalAddress(address)) {
        println(s"Launching follower $followerNumber on port ${followerSpec.port}")
        Seq(
            Seq("cd", followerSpec.workingDir),
            Seq("java", "-cp", followerSpec.classPath) ++ followerSpec.jvmOptions ++ Seq("orc.run.distrib.FollowerRuntime", followerNumber.toString, followerSpec.hostname+":"+followerSpec.port),
        )
      } else {
        val sshAddress = bindings.followerSockets.get(followerNumber - 1).getHostString
        println(s"Launching follower $followerNumber on $sshAddress:${followerSpec.port}")
        /* FIXME: Escape strings for shell */
        Seq(Seq("ssh", sshAddress, s"cd '${followerSpec.workingDir}' ; java -cp '${followerSpec.classPath}' ${followerSpec.jvmOptions.mkString(" ")} orc.run.distrib.FollowerRuntime $followerNumber ${followerSpec.hostname}:${followerSpec.port}"))
      }
      OsCommand.newTerminalWindowWith(commandSeq, s"Follower $followerNumber", 42, 132)
    }
  }

  def stopFollowers() {
    for (followerNumber <- 1 to bindings.followerSockets.size) {
      val address = bindings.followerSockets.get(followerNumber - 1).getAddress
      val port = bindings.followerSockets.get(followerNumber - 1).getPort
      if (isLocalAddress(address)) {
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
  
  case class DOrcRuntimePlacement(hostname: String, port: Int, workingDir: String, classPath:String, jvmOptions: Seq[String]) { }

//  val orcTestsProjDir = new File(".").getCanonicalPath
//  val dOrcTestDataDir = new File("test_data/distrib").getCanonicalPath
//  val orcVersion = orc.Main.versionProperties.getProperty("orc.version")
//  val dOrcClassPath = new File(s"../OrcScala/build/orc-${orcVersion}.jar").getCanonicalPath + ":" + new File("../OrcScala/lib").getCanonicalPath + "/*"
//  val jvmOpts = Seq("-Dsun.io.serialization.extendedDebugInfo=true")
//
//  val leaderSpec = 
//      DOrcRuntimePlacement("localhost", 0, orcTestsProjDir, dOrcClassPath, jvmOpts)
//
//  val followerSpecs = Seq(
//      //                  (hostname,    port,  workingDir,      classPath,     jvmOptions)
//      DOrcRuntimePlacement("localhost", 36721, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36722, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36723, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36724, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36725, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36726, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36727, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36728, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36729, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36730, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36731, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      DOrcRuntimePlacement("localhost", 36732, dOrcTestDataDir, dOrcClassPath, jvmOpts),
//      )

  val orcTestsProjDir = "/u/jthywiss/dorc-test/OrcTests"
  val dOrcTestDataDir = "/u/jthywiss/dorc-test/OrcTests/test_data/distrib"
  val orcVersion = orc.Main.versionProperties.getProperty("orc.version")
  val dOrcClassPath = s"/u/jthywiss/dorc-test/OrcScala/build/orc-${orcVersion}.jar:/u/jthywiss/dorc-test/OrcScala/lib/*"
  val jvmOpts = Seq("-Dsun.io.serialization.extendedDebugInfo=true")

  val leaderSpec = 
      DOrcRuntimePlacement("the-professor.cs.utexas.edu", 0, orcTestsProjDir, dOrcClassPath, jvmOpts)

  val followerSpecs = Seq(
      //                  (hostname,    port,  workingDir,     classPath,     jvmOptions)
      DOrcRuntimePlacement("gilligan.cs.utexas.edu", 36721, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("gilligan.cs.utexas.edu", 36722, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("ginger.cs.utexas.edu", 36723, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("ginger.cs.utexas.edu", 36724, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("lovey.cs.utexas.edu", 36725, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("lovey.cs.utexas.edu", 36726, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("mary-ann.cs.utexas.edu", 36727, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("mary-ann.cs.utexas.edu", 36728, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("skipper.cs.utexas.edu", 36729, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("skipper.cs.utexas.edu", 36730, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("thurston-howell-iii.cs.utexas.edu", 36731, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      DOrcRuntimePlacement("thurston-howell-iii.cs.utexas.edu", 36732, dOrcTestDataDir, dOrcClassPath, jvmOpts),
      )


  def buildSuite() = {
    val bindings = new orc.Main.OrcCmdLineOptions()
    bindings.backend = DistributedBackendType
    bindings.followerSockets = (followerSpecs map { fs => new InetSocketAddress(fs.hostname, fs.port)}).asJava
    bindings.showJavaStackTrace = true
    TestUtils.buildSuite(classOf[DistribTest].getSimpleName(), classOf[DistribTestCase], bindings, new File("test_data/distrib"))
  }

}
