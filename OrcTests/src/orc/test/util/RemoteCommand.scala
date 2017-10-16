//
// RemoteCommand.scala -- Scala object RemoteCommand
// Project OrcTests
//
// Created by amp on Oct, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.File

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
    checkExitValue(s"mkdir -p $remoteDirname on $remoteHostname", OsCommand.getResultFrom(Seq("ssh", remoteHostname, s"mkdir -p $remoteDirname")))
  }
  
  @throws[RemoteCommandException]
  def deleteDirectory(remoteHostname: String, remoteDirname: String): Unit = {
    checkExitValue(s"rm -rf $remoteDirname on $remoteHostname", OsCommand.getResultFrom(Seq("ssh", remoteHostname, s"rm -rf $remoteDirname")))
  }
  
  @throws[RemoteCommandException]
  def rsyncToRemote(localFilename: String, remoteHostname: String, remoteFilename: String): Unit = {
    val localFile = new File(localFilename)
    val localFileCanonicalName = localFile.getCanonicalPath + (if (localFile.isDirectory) "/" else "")
    checkExitValue(s"rsync of $localFileCanonicalName to $remoteHostname:$remoteFilename", OsCommand.getResultFrom(Seq("rsync", "-rlpt", localFileCanonicalName, s"${remoteHostname}:${remoteFilename}")))
  }

  @throws[RemoteCommandException]
  def rsyncFromRemote(remoteHostname: String, remoteFilename: String, localFilename: String): Unit = {
    val localFile = new File(localFilename)
    val localFileCanonicalName = localFile.getCanonicalPath + (if (localFile.isDirectory) "/" else "")
    checkExitValue(s"rsync of $remoteHostname:$remoteFilename to $localFileCanonicalName", OsCommand.getResultFrom(Seq("rsync", "-rlpt", s"${remoteHostname}:${remoteFilename}", localFileCanonicalName)))
  }

  
  @throws[RemoteCommandException]
  def checkExitValue(description: String, result: OsCommandResult): Unit = {
    if (result.exitStatus != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new RemoteCommandException(s"${description} failed: exitStatus=${result.exitStatus}, stderr=${result.stderr}")
    }
  }
}

class RemoteCommandException(message: String, cause: Throwable) extends OsCommandException(message, cause) {
  def this(message: String) = this(message, null)
}
