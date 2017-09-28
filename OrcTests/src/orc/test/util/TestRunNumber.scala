//
// TestRunNumber.scala -- Scala object TestRunNumber
// Project OrcTests
//
// Created by jthywiss on Sep 16, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
    val runCounterFilename = Config.get("testRunNumberFile")
    /*FIXME: Need a better way to find the script.  This depends on details of our build procedure. */
    val getNewRunNumCmdName = Config.get("testRunNumberCommandName").getOrElse("build/orc/test/util/get-new-run-seq-num.sh")
    val getNewRunNumCmd = if (runCounterFilename.isDefined) Seq(getNewRunNumCmdName, runCounterFilename.get) else Seq(getNewRunNumCmdName)
    val result = Config.get("testRunNumberServer") match {
      case None => OsCommand.getResultFrom(getNewRunNumCmd)
      case Some(hostname) => OsCommand.getResultFrom(Seq("ssh", hostname) ++ getNewRunNumCmd)
    }
    if (result.exitValue != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new TestRunNumberCmdException(s"${getNewRunNumCmd.mkString(" ")} failed: exitValue=${result.exitValue}, stderr=${result.stderr}")
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
