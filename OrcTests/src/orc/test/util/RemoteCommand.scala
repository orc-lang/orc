//
// RemoteCommand.scala -- Scala object RemoteCommand and class RemoteCommandException
// Project OrcTests
//
// Created by amp on Oct, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ IOException, UnsupportedEncodingException }
import java.net.{ InetAddress, NetworkInterface, SocketException }
import java.nio.file.{ Files, Paths }

/** Utility methods to interact with remote systems.
  *
  * This include both file copying and running commands remotely.
  *
  * @author jthywiss
  */
object RemoteCommand {

  @throws[RemoteCommandException]
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def mkdirAndRsync(localPathname: String, remoteHostname: String, remotePathname: String): Unit = {
    val localFile = Paths.get(localPathname)
    val remoteDirPath = if (Files.isDirectory(localFile)) remotePathname else Paths.get(remotePathname).getParent.toString
    mkdir(remoteHostname, remoteDirPath)
    rsyncToRemote(localPathname, remoteHostname, remotePathname)
  }

  @throws[RemoteCommandException]
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def mkdir(remoteHostname: String, remoteDirname: String): Unit = {
    checkExitValue(s"mkdir -p $remoteDirname on $remoteHostname", runAndGetResult(remoteHostname, Seq("mkdir", "-p", remoteDirname)))
  }

  @throws[RemoteCommandException]
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def deleteDirectory(remoteHostname: String, remoteDirname: String): Unit = {
    checkExitValue(s"rm -rf $remoteDirname on $remoteHostname", runAndGetResult(remoteHostname, Seq("rm", "-rf", remoteDirname)))
  }

  @throws[RemoteCommandException]
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def rsyncToRemote(localPathname: String, remoteHostname: String, remotePathname: String): Unit = {
    val localFile = Paths.get(localPathname)
    val localFilePathname = localFile.toAbsolutePath.toString + (if (Files.isDirectory(localFile)) "/" else "")
    checkExitValue(
      s"rsync of $localFilePathname to $remoteHostname:$remotePathname",
      OsCommand.runAndGetResult(Seq("rsync", "-rlpt", "--exclude=.orcache", localFilePathname, s"${remoteHostname}:${remotePathname}")))
  }

  @throws[RemoteCommandException]
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def rsyncFromRemote(remoteHostname: String, remotePathname: String, localPathname: String): Unit = {
    val localFile = Paths.get(localPathname)
    val localFilePathname = localFile.toAbsolutePath.toString + (if (Files.isDirectory(localFile)) "/" else "")
    checkExitValue(s"rsync of $remoteHostname:$remotePathname to $localFilePathname", OsCommand.runAndGetResult(Seq("rsync", "-rlpt", s"${remoteHostname}:${remotePathname}", localFilePathname)))
  }

  /** Run the given command, with an empty stdin, and using this process'
    * stdout and stderr.  Return the command's exit value.
    */
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def runWithEcho(remoteHostname: String, command: Seq[String], remoteWorkingDir: String): Int = {
    OsCommand.run(Seq("ssh", remoteHostname, s"cd '${remoteWorkingDir}'; ${OsCommand.toQuotedShellWords(command)}"))
  }

  /** Run the given command, with an empty stdin, saving stdout and stderr to
    * the given remote files, and echoing stdout and stderr to this process'
    * stdout and stderr.  Return the command's exit value.
    */
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def runWithEcho(remoteHostname: String, command: Seq[String], remoteWorkingDir: String, remoteOutFile: String, remoteErrFile: String): Int = {
    OsCommand.run(Seq("ssh", remoteHostname, s"cd '${remoteWorkingDir}'; { { ${OsCommand.toQuotedShellWords(command)} | tee '$remoteOutFile'; exit $${PIPESTATUS[0]}; } 2>&1 1>&3 | tee '$remoteErrFile'; exit $${PIPESTATUS[0]}; } 3>&1 1>&2"))
  }

  /** Run the given command, with an empty stdin, saving stdout and stderr to
    * the given remote files.  Return the command's Process instance.
    */
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def runNoEcho(remoteHostname: String, command: Seq[String], remoteWorkingDir: String, remoteOutFile: String, remoteErrFile: String): Process = {
    OsCommand.runNoWait(Seq("ssh", remoteHostname, s"cd '${remoteWorkingDir}' >'$remoteOutFile' 2>'$remoteErrFile'; ${OsCommand.toQuotedShellWords(command)} >'$remoteOutFile' 2>'$remoteErrFile'"))
  }

  /** Run the given command, saving stdout and stderr, with an empty stdin.
    *
    * Note: This should be used for commands with known small amounts of
    * output, since all of stdout and stderr will be buffered and returned
    * as part of the OsCommandResult value.
    *
    * @see OsCommandResult
    */
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def runAndGetResult(remoteHostname: String, command: Seq[String], remoteWorkingDir: String): OsCommandResult = {
    OsCommand.runAndGetResult(Seq("ssh", remoteHostname, s"cd '${remoteWorkingDir}'; ${OsCommand.toQuotedShellWords(command)}"))
  }

  /** Run the given command, saving stdout and stderr, with an empty stdin.
    *
    * Note: This should be used for commands with known small amounts of
    * output, since all of stdout and stderr will be buffered and returned
    * as part of the OsCommandResult value.
    *
    * @see OsCommandResult
    */
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def runAndGetResult(remoteHostname: String, command: Seq[String]): OsCommandResult = {
    OsCommand.runAndGetResult(Seq("ssh", remoteHostname, OsCommand.toQuotedShellWords(command)))
  }

  @throws[RemoteCommandException]
  def checkExitValue(description: String, result: OsCommandResult): Unit = {
    if (result.exitStatus != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new RemoteCommandException(s"${description} failed: exitStatus=${result.exitStatus}, stderr=${result.stderr}")
    }
  }

  def isLocalAddress(address: InetAddress): Boolean = {
    (address == null) || address.isLoopbackAddress || address.isAnyLocalAddress ||
      (try {
        NetworkInterface.getByInetAddress(address) != null
      } catch {
        case _: SocketException => false
      })
  }

}

class RemoteCommandException(message: String, cause: Throwable) extends OsCommandException(message, cause) {
  def this(message: String) = this(message, null)
}
