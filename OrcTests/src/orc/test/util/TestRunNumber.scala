//
// TestRunNumber.scala -- Scala object TestRunNumber
// Project OrcTests
//
// Created by jthywiss on Sep 16, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

/** Get a new run sequence number by invoking the get-new-run-seq-num.sh
  * shell script.
  *
  * @author jthywiss
  */
object TestRunNumber {

  def getNext(): String = {
    val runCounterPathname = Config.get("testRunNumberFile")
    /*FIXME: Need a better way to find the script.  This depends on details of our build procedure. */
    val getNewRunNumCmdName = Config.get("testRunNumberCommandName").getOrElse("build/orc/test/util/get-new-run-seq-num.sh")
    val getNewRunNumCmd = if (runCounterPathname.isDefined) Seq(getNewRunNumCmdName, runCounterPathname.get) else Seq(getNewRunNumCmdName)
    val result = Config.get("testRunNumberServer") match {
      case None => OsCommand.runAndGetResult(getNewRunNumCmd)
      case Some(hostname) => RemoteCommand.runAndGetResult(hostname, getNewRunNumCmd)
    }
    if (result.exitStatus != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new TestRunNumberCmdException(s"${getNewRunNumCmd.mkString(" ")} failed: exitStatus=${result.exitStatus}, stderr=${result.stderr}")
    }
    result.stdout.stripLineEnd
  }

  /** Unique and constant test run sequence number for this JVM instance. */
  lazy val singletonNumber = getNext()

  object Config extends orc.util.Config("TestRunNumberConfig") {}

  def main(args: Array[String]): Unit = {
    println(singletonNumber)
  }
}

private class TestRunNumberCmdException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}
