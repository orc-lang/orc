//
// OsCommand.scala -- Scala object OsCommand and class OsCommandResult
// Project OrcTests
//
// Created by jthywiss on Jul 18, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ ByteArrayOutputStream, File, InputStream, OutputStream }
import java.nio.charset.{ Charset, StandardCharsets }

import scala.collection.JavaConverters.seqAsJavaListConverter

/** Utility methods to invoke commands of the underlying OS.
  *
  * @author jthywiss
  */
object OsCommand {

  def getResultFrom(command: Seq[String], stdin: String = "", directory: File = null, charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: OutputStream = java.lang.System.out, stderrTee: OutputStream = java.lang.System.err) = {
    val pb = new ProcessBuilder(command.asJava)
    if (directory != null) pb.directory(directory)
    val p = pb.start()

    val stdinStream = p.getOutputStream /* sic. yes, confusing naming */
    stdinStream.write(stdin.getBytes(charset.name))
    stdinStream.close()

    val outBAOS = new ByteArrayOutputStream()
    /* Copy process stdout to outBAOS and optionally our stdout */
    val outDrainThread = new StreamDrainThread(p.getInputStream, outBAOS, if (teeStdOutErr) Some(stdoutTee) else None, "Subprocess stdout reader")
    outDrainThread.start()

    val errBAOS = new ByteArrayOutputStream()
    /* Copy process stderr to errBAOS and optionally our stderr */
    val errDrainThread = new StreamDrainThread(p.getErrorStream, errBAOS, if (teeStdOutErr) Some(stderrTee) else None, "Subprocess stderr reader")
    errDrainThread.start()

    val exitValue = p.waitFor()

    outDrainThread.join(400 /*ms*/)
    errDrainThread.join(400 /*ms*/)
    if (outDrainThread.isAlive) Logger.warning("outDrainThread should be dead for "+command)
    if (errDrainThread.isAlive) Logger.warning("errDrainThread should be dead for "+command)

    val stdoutString = outBAOS.toString(charset.name)
    val stderrString = errBAOS.toString(charset.name)

    OsCommandResult(exitValue, stdoutString, stderrString)
  }

}


private class StreamDrainThread(sourceStream: InputStream, targetStream1: OutputStream, targetStream2: Option[OutputStream], name: String) extends Thread(name) {
  setDaemon(true)

  override def run() {
    val buff = new Array[Byte](8192)
    var bytesRead = 0

    do {
      bytesRead = sourceStream.read(buff)
      if (bytesRead > 0) {
        targetStream2 match {
          case Some(outStream) => {
            outStream.write(buff, 0, bytesRead)
            outStream.flush()
          }
          case None => { }
        }
        targetStream1.write(buff, 0, bytesRead)
        targetStream1.flush()
      }
    } while (bytesRead > 0)
  }
}


case class OsCommandResult(val exitValue: Int, val stdout: String, val stderr: String)
