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
import java.net.InetSocketAddress

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
      val actual = OrcForTesting.compileAndRun(orcFile.getPath(), OrcTestCase.TESTING_TIMEOUT, bindings)
          if (!expecteds.contains(actual)) {
            throw new AssertionError("Unexpected output:\n" + actual);
          }
    } catch {
      case fnse: FeatureNotSupportedException => Assume.assumeNoException(fnse)
      case ce: CompilationException => throw new AssertionError(ce.getMessageAndDiagnostics())
    } finally {
      println()
      stopFollowers()
      println()
    }
  }

  def startFollowers() {
    val dOrcWorkingDir = new File("test_data/distrib").getCanonicalPath
    val orcVersion = orc.Main.versionProperties.getProperty("orc.version")
    val dOrcClassPath = new File(s"../OrcScala/build/orc-${orcVersion}.jar").getCanonicalPath+":"+new File("../OrcScala/lib").getCanonicalPath+"/*"
    val jvmOpts = "-Dsun.io.serialization.extendedDebugInfo=true"

    for (followerNumber <- 1 to bindings.followerSockets.size) {
      val port = bindings.followerSockets.get(followerNumber - 1).getPort()
      val commandSeq = Seq(
          Seq("cd",dOrcWorkingDir),
          Seq("java","-cp",dOrcClassPath,jvmOpts,"orc.run.distrib.FollowerRuntime",followerNumber.toString, port.toString)
        )
      println(s"Launching follower $followerNumber on port $port")
      OsCommand.newTerminalWindowWith(commandSeq, s"Follower $followerNumber", 42, 132)
    }
  }
  
  def stopFollowers() {
    for (followerNumber <- 1 to bindings.followerSockets.size) {
      val port = bindings.followerSockets.get(followerNumber - 1).getPort()
      val lsofResult = OsCommand.getResultFrom(Seq("lsof","-t","-a",s"-i:$port","-sTCP:LISTEN"))
      if (lsofResult.exitValue == 0) {
        println(s"Terminating follower $followerNumber on port $port")
        OsCommand.getResultFrom(Seq("kill",lsofResult.stdout.stripLineEnd))
      } else {
        (s"Unable to terminate follower $followerNumber on port $port")
      }
    }
  }

}

object DistribTestCase {
  def buildSuite() = {
    val bindings = new OrcBindings()
    bindings.backend = DistributedBackendType
    bindings.followerSockets = Seq(new InetSocketAddress("localhost", 36721), new InetSocketAddress("localhost", 36722)).asJava
    TestUtils.buildSuite(classOf[DistribTest].getSimpleName(), classOf[DistribTestCase], bindings, new File("test_data/distrib"))    
  }
}
