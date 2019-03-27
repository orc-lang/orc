//
// OsCommand.scala -- Scala object OsCommand and classes OsCommandResult and OsCommandException
// Project OrcTests
//
// Created by jthywiss on Jul 18, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ ByteArrayOutputStream, File, InputStream, OutputStream }
import java.nio.charset.{ Charset, StandardCharsets }

import scala.collection.JavaConverters.seqAsJavaListConverter
import java.util.concurrent.TimeUnit

/** Utility methods to invoke commands of the underlying OS.
  *
  * @author jthywiss
  */
object OsCommand {

  /** Run the given command, either with the given string as stdin, or an
    * empty stdin; and using this process' stdout and stderr, or to the
    * given Files.  Return the command's exit value.
    */
  def run(command: Seq[String], directory: File = null, stdin: String = "", stdout: File = null, stderr: File = null, charset: Charset = StandardCharsets.UTF_8) = {
    val p = runNoWait(command, directory, stdin, stdout, stderr, charset)

    val exitStatus = p.waitFor()

    exitStatus
  }

  /** Run the given command, either with the given string as stdin, or an
    * empty stdin; and using this process' stdout and stderr, or to the
    * given Files.  Return the command's Process instance.
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

  /** Run the given command, either with the given string as stdin, or
    * an empty stdin.  Return the command's exit value.
    */
  def getStatusFrom(command: Seq[String], directory: File = null, stdin: String = "", charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: Traversable[OutputStream] = Seq(System.out), stderrTee: Traversable[OutputStream] = Seq(System.err), timeout: Option[Double] = None) = {
    val pb = new ProcessBuilder(command.asJava)
    if (directory != null) pb.directory(directory)
    /* Lead pb's output & error redirect set as Redirect.PIPE */
    val p = pb.start()

    val stdinStream = p.getOutputStream /* sic. yes, confusing naming */
    stdinStream.write(stdin.getBytes(charset.name))
    stdinStream.close()

    val outs = (if (teeStdOutErr) stdoutTee else Seq())
    /* Copy process stdout to given destinations */
    val outDrainThread = new StreamDrainThread(p.getInputStream, outs, "Subprocess stdout reader")
    outDrainThread.start()

    val errs = (if (teeStdOutErr) stderrTee else Seq())
    /* Copy process stderr to given destinations */
    val errDrainThread = new StreamDrainThread(p.getErrorStream, errs, "Subprocess stderr reader")
    errDrainThread.start()

    val exitStatus = timeout match {
      case Some(t) =>
          if (p.waitFor((t * 1000).toInt, TimeUnit.MILLISECONDS)) {
            p.exitValue()
          } else {
            p.destroyForcibly()
            -1
          }
      case None =>
        p.waitFor()
    }

    outDrainThread.join(400 /*ms*/)
    errDrainThread.join(400 /*ms*/)
    if (outDrainThread.isAlive) Logger.warning("outDrainThread should be dead for "+command)
    if (errDrainThread.isAlive) Logger.warning("errDrainThread should be dead for "+command)

    exitStatus
  }

  /** Run the given command, saving stdout and stderr, and either with the
    * given string as stdin, or an empty stdin.
    */
  def getResultFrom(command: Seq[String], directory: File = null, stdin: String = "", charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: Traversable[OutputStream] = Seq(System.out), stderrTee: Traversable[OutputStream] = Seq(System.err), timeout: Option[Double] = None) = {

    val outBAOS = new ByteArrayOutputStream()
    val errBAOS = new ByteArrayOutputStream()

    val outs = (if (teeStdOutErr) stdoutTee else Seq()) ++ Seq(outBAOS)
    val errs = (if (teeStdOutErr) stderrTee else Seq()) ++ Seq(errBAOS)
    val exitStatus = getStatusFrom(command, directory, stdin, charset, true, outs, errs, timeout)

    val stdoutString = outBAOS.toString(charset.name)
    val stderrString = errBAOS.toString(charset.name)

    OsCommandResult(exitStatus, stdoutString, stderrString)
  }

  @throws[OsCommandException]
  def checkExitValue(description: String, result: OsCommandResult): Unit = {
    if (result.exitStatus != 0) {
      print(result.stdout)
      Console.err.print(result.stderr)
      throw new OsCommandException(s"${description} failed: exitStatus=${result.exitStatus}, stderr=${result.stderr}")
    }
  }

  /** Quotes the given string such that shell parsing will not split it into
    *  multiple tokens. WARNING: This does not remove all special meaning of
    *  characters, such as parameter/variable expansions, globbing, comments,
    *  etc.
    */
  def quoteShellToken(str: String): String = {
    def escape(ch: Char) = if ("\t\n &();<>|".contains(ch)) "\\" + ch else ch.toString
    str.flatMap(escape(_))
  }

  /** Quotes the given string such that shell parsing will not affect it. */
  def quoteShellLiterally(str: String): String = {
    "'" + str.replaceAllLiterally("'", "'\''") + "'"
  }

  /** Quotes the given string such that shell parsing does not affect it,
    * except parameter/variable expansion ${...}, command substitution $(...)
    * and `...`, and arithmetic expansion $((...)) are still performed.
    */
  def quoteShellAllowExpansion(str: String): String = {
    "\"" + str.replaceAllLiterally("\"", "\\\"") + "\""
  }

}

class OsCommandException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
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
