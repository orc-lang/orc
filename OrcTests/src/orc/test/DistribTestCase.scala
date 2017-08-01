//
// DistribTestCase.scala -- Scala class DistribTestCase
// Project project_name
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
import orc.script.OrcBindings
import orc.test.TestUtils.OrcTestCase

import org.junit.Assume

/** JUnit test case for a distributed-Orc test.
  *
  * @author jthywiss
  */
class DistribTestCase extends OrcTestCase {
  @throws(classOf[Throwable])
  override def runTest() {
    startFollowers()
    println("\n==== Starting " + orcFile + " ====")
    try {
      val actual = OrcForTesting.compileAndRun(orcFile.getPath(), 200 /*s*/, bindings)
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
    address.isLoopbackAddress || address.isAnyLocalAddress ||
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
            Seq("java", "-cp", followerSpec.classPath) ++ followerSpec.jvmOptions ++ Seq("orc.run.distrib.FollowerRuntime", followerNumber.toString, followerSpec.port.toString),
        )
      } else {
        val sshAddress = bindings.followerSockets.get(followerNumber - 1).getHostString
        println(s"Launching follower $followerNumber on $sshAddress:${followerSpec.port}")
        /* FIXME: Escape strings for shell */
        Seq(Seq("ssh", sshAddress, s"cd '${followerSpec.workingDir}' ; java -cp '${followerSpec.classPath}' ${followerSpec.jvmOptions.mkString(" ")} orc.run.distrib.FollowerRuntime $followerNumber ${followerSpec.port}"))
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
  
  val dOrcWorkingDir = new File("test_data/distrib").getCanonicalPath
  val orcVersion = orc.Main.versionProperties.getProperty("orc.version")
  val dOrcClassPath = new File(s"../OrcScala/build/orc-${orcVersion}.jar").getCanonicalPath + ":" + new File("../OrcScala/lib").getCanonicalPath + "/*"
  val jvmOpts = Seq("-Dsun.io.serialization.extendedDebugInfo=true")

  case class FollowerSpec(hostname: String, port: Int, workingDir: String, classPath:String, jvmOptions: Seq[String]) { }

  val followerSpecs = Seq(
      //          (hostname,    port,  workingDir,     classPath,     jvmOptions)
      FollowerSpec("localhost", 36721, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36722, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36723, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36724, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36725, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36726, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36727, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36728, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36729, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36730, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36731, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      FollowerSpec("localhost", 36732, dOrcWorkingDir, dOrcClassPath, jvmOpts),
      )
  
  def buildSuite() = {
    val bindings = new OrcBindings()
    bindings.backend = DistributedBackendType
    bindings.followerSockets = (followerSpecs map { fs => new InetSocketAddress(fs.hostname, fs.port)}).asJava
    bindings.showJavaStackTrace = true
    TestUtils.buildSuite(classOf[DistribTest].getSimpleName(), classOf[DistribTestCase], bindings, new File("test_data/distrib"))
  }

}
