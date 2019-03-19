//
// OsCommand.scala -- Scala object OsCommand and classes OsCommandResult and OsCommandException
// Project OrcTests
//
// Created by jthywiss on Jul 18, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import java.io.{ ByteArrayOutputStream, IOException, InputStream, OutputStream, UnsupportedEncodingException }
import java.nio.charset.{ Charset, StandardCharsets }
import java.nio.file.Path

import scala.collection.JavaConverters.seqAsJavaListConverter

/** Utility methods to invoke commands of the underlying OS.
  *
  * @author jthywiss
  */
object OsCommand {

  /** Run the given command, either with the given string as stdin, or an
    * empty stdin; and using this process' stdout and stderr, or to the
    * given Files.  Return the command's exit value.
    *
    * If more sophisticated stdout and stderr redirection is needed, use the
    * runAndGetStatus method.
    *
    * @see #runAndGetStatus
    */
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def run(command: Seq[String], workingDir: Path = null, stdin: String = "", stdout: Path = null, stderr: Path = null, charset: Charset = StandardCharsets.UTF_8): Int = {
    val p = runNoWait(command, workingDir, stdin, stdout, stderr, charset)

    val exitStatus = p.waitFor()

    exitStatus
  }

  /** Run the given command, either with the given string as stdin, or an
    * empty stdin; and using this process' stdout and stderr, or to the
    * given Files.  Return the command's Process instance.
    */
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def runNoWait(command: Seq[String], workingDir: Path = null, stdin: String = "", stdout: Path = null, stderr: Path = null, charset: Charset = StandardCharsets.UTF_8): Process = {
    val pb = new ProcessBuilder(command.asJava)
    if (workingDir != null) pb.directory(workingDir.toFile)
    pb.redirectOutput(if (stdout == null) ProcessBuilder.Redirect.INHERIT else ProcessBuilder.Redirect.to(stdout.toFile))
    pb.redirectError(if (stderr == null) ProcessBuilder.Redirect.INHERIT else ProcessBuilder.Redirect.to(stderr.toFile))
    val p = pb.start()

    val stdinStream = p.getOutputStream /* sic. yes, confusing naming */
    stdinStream.write(stdin.getBytes(charset.name))
    stdinStream.close()

    p
  }

  /** Run the given command, either with the given string as stdin, or
    * an empty stdin.  Return the command's exit value.
    *
    * This differs from the run method in its support for stdout and stderr
    * redirection to possibly multiple OutputStreams.  For simple cases,
    * just use the run method.
    *
    * @see #run
    */
  @throws[InterruptedException]
  @throws[IOException]
  @throws[UnsupportedEncodingException]
  def runAndGetStatus(command: Seq[String], workingDir: Path = null, stdin: String = "", charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: Traversable[OutputStream] = Seq(System.out), stderrTee: Traversable[OutputStream] = Seq(System.err)): Int = {
    val pb = new ProcessBuilder(command.asJava)
    if (workingDir != null) pb.directory(workingDir.toFile)
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

    val exitStatus = p.waitFor()

    outDrainThread.join(400 /*ms*/)
    errDrainThread.join(400 /*ms*/)
    if (outDrainThread.isAlive) Logger.warning("outDrainThread should be dead for " + command)
    if (errDrainThread.isAlive) Logger.warning("errDrainThread should be dead for " + command)

    exitStatus
  }

  /** Run the given command, saving stdout and stderr, and either with the
    * given string as stdin, or an empty stdin.
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
  def runAndGetResult(command: Seq[String], workingDir: Path = null, stdin: String = "", charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false, stdoutTee: Traversable[OutputStream] = Seq(System.out), stderrTee: Traversable[OutputStream] = Seq(System.err)): OsCommandResult = {

    val outBAOS = new ByteArrayOutputStream()
    val errBAOS = new ByteArrayOutputStream()

    val outs = (if (teeStdOutErr) stdoutTee else Seq()) ++ Seq(outBAOS)
    val errs = (if (teeStdOutErr) stderrTee else Seq()) ++ Seq(errBAOS)
    val exitStatus = runAndGetStatus(command, workingDir, stdin, charset, true, outs, errs)

    val stdoutString = outBAOS.toString(charset.name)
    val stderrString = errBAOS.toString(charset.name)

    OsCommandResult(exitStatus, stdoutString, stderrString)
  }

  /** If the given OsCommandResult exit status is non-zero, throw an OsCommandException */
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

  /** Transform the given command (string sequence) to a space-separated
    * string that the shell would parse as the given command.  E.g.:
    *  {"a", "b b b", "c'c"} -> "'a' 'b b b' 'c'\''c'"
    */
  def toQuotedShellWords(command: Seq[String]): String = {
    command.map(OsCommand.quoteShellLiterally(_)).mkString(" ")
  }

  private class StreamDrainThread(sourceStream: InputStream, targetStreams: Traversable[OutputStream], name: String) extends Thread(name) {
    setDaemon(true)

    override def run(): Unit = {
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

}

case class OsCommandResult(val exitStatus: Int, val stdout: String, val stderr: String)

class OsCommandException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}
