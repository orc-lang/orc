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

import java.io.File
import java.net.{ InetAddress, NetworkInterface, SocketException }

/** Utility methods to interact with remote systems.
  *
  * This include both file copying and running commands remotely.
  *
  * @author jthywiss
  */
object RemoteCommand {

  @throws[RemoteCommandException]
  def mkdirAndRsync(localFilename: String, remoteHostname: String, remoteFilename: String): Unit = {
    val localFile = new File(localFilename)
    val remoteDirPath = if (localFile.isDirectory) remoteFilename else new File(remoteFilename).getParent
    mkdir(remoteHostname, remoteDirPath)
    rsyncToRemote(localFilename, remoteHostname, remoteFilename)
  }

  @throws[RemoteCommandException]
  def mkdir(remoteHostname: String, remoteDirname: String): Unit = {
    checkExitValue(s"mkdir -p $remoteDirname on $remoteHostname", OsCommand.runAndGetResult(Seq("ssh", remoteHostname, s"mkdir -p $remoteDirname")))
  }

  @throws[RemoteCommandException]
  def deleteDirectory(remoteHostname: String, remoteDirname: String): Unit = {
    checkExitValue(s"rm -rf $remoteDirname on $remoteHostname", OsCommand.runAndGetResult(Seq("ssh", remoteHostname, s"rm -rf $remoteDirname")))
  }

  @throws[RemoteCommandException]
  def rsyncToRemote(localFilename: String, remoteHostname: String, remoteFilename: String): Unit = {
    val localFile = new File(localFilename)
    val localFileCanonicalName = localFile.getCanonicalPath + (if (localFile.isDirectory) "/" else "")
    checkExitValue(
      s"rsync of $localFileCanonicalName to $remoteHostname:$remoteFilename",
      OsCommand.runAndGetResult(Seq("rsync", "-rlpt", "--exclude=.orcache", localFileCanonicalName, s"${remoteHostname}:${remoteFilename}")))
  }

  @throws[RemoteCommandException]
  def rsyncFromRemote(remoteHostname: String, remoteFilename: String, localFilename: String): Unit = {
    val localFile = new File(localFilename)
    val localFileCanonicalName = localFile.getCanonicalPath + (if (localFile.isDirectory) "/" else "")
    checkExitValue(s"rsync of $remoteHostname:$remoteFilename to $localFileCanonicalName", OsCommand.runAndGetResult(Seq("rsync", "-rlpt", s"${remoteHostname}:${remoteFilename}", localFileCanonicalName)))
  }

  @throws[RemoteCommandException]
  def checkExitValue(description: String, result: OsCommandResult): Unit = {
    if (result.exitStatus != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new RemoteCommandException(s"${description} failed: exitStatus=${result.exitStatus}, stderr=${result.stderr}")
    }
  }

  //TODO
  //def runRemote(hostname: String, command: Seq[String], directory: File = null, stdin: String = "", stdout: File = null, stderr: File = null, charset: Charset = StandardCharsets.UTF_8) = {
  //  //... quote command properly ...
  //  OsCommand.run(Seq("ssh", hostname, s"cd $directory; command >stdout 2>stderr"), null, stdin, charset)
  //}

  //TODO
  //def getRemoteResultFrom(hostname: String, command: Seq[String], directory: File = null, stdin: String = "", charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: OutputStream = java.lang.System.out, stderrTee: OutputStream = java.lang.System.err) = {
  //  //... quote command properly ...
  //  OsCommand.runAndGetResult(Seq("ssh", hostname, s"cd $directory; command >stdout 2>stderr"), null, stdin, charset, teeStdOutErr, stdoutTee, stderrTee)
  //}

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
