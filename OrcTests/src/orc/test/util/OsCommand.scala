//
// OsCommand.scala -- Scala object OsCommand and classes OsCommandResult and OSCommandException
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

  /** Run the given command, either with the given string as stdin, or an
    * empty stdin; and using this process' stdout and stderr, or to the
    * given Files.  Returns the command's exit value.
    */
  def run(command: Seq[String], directory: File = null, stdin: String = "", stdout: File = null, stderr: File = null, charset: Charset = StandardCharsets.UTF_8) = {
    val p = runNoWait(command, directory, stdin, stdout, stderr, charset)

    val exitStatus = p.waitFor()

    exitStatus
  }

  /** Run the given command, either with the given string as stdin, or an
    * empty stdin; and using this process' stdout and stderr, or to the
    * given Files.  Returns the command's Process instance.
    */
  def runNoWait(command: Seq[String], directory: File = null, stdin: String = "", stdout: File = null, stderr: File = null, charset: Charset = StandardCharsets.UTF_8) = {
    val pb = new ProcessBuilder(command.asJava)
    if (directory != null) pb.directory(directory)
    pb.redirectOutput(if (stdout == null) ProcessBuilder.Redirect.INHERIT else ProcessBuilder.Redirect.to(stdout))
    pb.redirectError(if (stderr == null) ProcessBuilder.Redirect.INHERIT else ProcessBuilder.Redirect.to(stderr))
    val p = pb.start()

    val stdinStream = p.getOutputStream /* sic. yes, confusing naming */
    stdinStream.write(stdin.getBytes(charset.name))
    stdinStream.close()

    p
  }

  /** Run the given command, saving stdout and stderr, and either with the
    * given string as stdin, or an empty stdin.
    */
  def getResultFrom(command: Seq[String], directory: File = null, stdin: String = "", charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: Traversable[OutputStream] = Seq(System.out), stderrTee: Traversable[OutputStream] = Seq(System.err)) = {
    val pb = new ProcessBuilder(command.asJava)
    if (directory != null) pb.directory(directory)
    val p = pb.start()

    val stdinStream = p.getOutputStream /* sic. yes, confusing naming */
    stdinStream.write(stdin.getBytes(charset.name))
    stdinStream.close()

    val outBAOS = new ByteArrayOutputStream()
    val outs = (if (teeStdOutErr) stdoutTee else Seq()) ++ Seq(outBAOS)
    /* Copy process stdout to outBAOS and optionally our stdout */
    val outDrainThread = new StreamDrainThread(p.getInputStream, outs, "Subprocess stdout reader")
    outDrainThread.start()

    val errBAOS = new ByteArrayOutputStream()
    val errs = (if (teeStdOutErr) stderrTee else Seq()) ++ Seq(errBAOS)
    /* Copy process stderr to errBAOS and optionally our stderr */
    val errDrainThread = new StreamDrainThread(p.getErrorStream, errs, "Subprocess stderr reader")
    errDrainThread.start()

    val exitStatus = p.waitFor()

    outDrainThread.join(400 /*ms*/)
    errDrainThread.join(400 /*ms*/)
    if (outDrainThread.isAlive) Logger.warning("outDrainThread should be dead for "+command)
    if (errDrainThread.isAlive) Logger.warning("errDrainThread should be dead for "+command)

    val stdoutString = outBAOS.toString(charset.name)
    val stderrString = errBAOS.toString(charset.name)

    OsCommandResult(exitStatus, stdoutString, stderrString)
  }


  
  @throws[OSCommandException]
  def checkExitValue(description: String, result: OsCommandResult): Unit = {
    if (result.exitStatus != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new OSCommandException(s"${description} failed: exitStatus=${result.exitStatus}, stderr=${result.stderr}")
    }
  }
}

class OSCommandException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}

private class StreamDrainThread(sourceStream: InputStream, targetStreams: Traversable[OutputStream], name: String) extends Thread(name) {
  setDaemon(true)

  override def run() {
    val buff = new Array[Byte](8192)
    var bytesRead = 0

    do {
      bytesRead = sourceStream.read(buff)
      if (bytesRead > 0) {
        for (outStream <- targetStreams) {
          outStream.write(buff, 0, bytesRead)
          outStream.flush()
        }
      }
    } while (bytesRead > 0)
  }
}


case class OsCommandResult(val exitStatus: Int, val stdout: String, val stderr: String)
