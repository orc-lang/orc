//
// OsCommand.scala -- Scala objects OsCommand and OsType, and class OsCommandResult
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

package orc.test

import java.io.{ ByteArrayOutputStream, File, InputStream, OutputStream }
import java.nio.charset.{ Charset, StandardCharsets }

import scala.collection.JavaConverters.seqAsJavaListConverter

/** Utility methods to invoke commands of the underlying OS.
  *
  * @author jthywiss
  */
object OsCommand {

  def getResultFrom(command: Seq[String], stdin: String = "", directory: File = null, charset: Charset = StandardCharsets.UTF_8, teeStdOutErr: Boolean = false) = {
    val pb = new ProcessBuilder(command.asJava)
    if (directory != null) pb.directory(directory)
    val p = pb.start()

    val stdinStream = p.getOutputStream /* sic. yes, confusing naming */
    stdinStream.write(stdin.getBytes(charset.name))
    stdinStream.close()

    val outBAOS = new ByteArrayOutputStream()
    /* Copy process stdout to outBAOS and optionally our stdout */
    val outDrainThread = new StreamDrainThread(p.getInputStream, outBAOS, if (teeStdOutErr) Some(java.lang.System.out) else None, "Subprocess stdout reader")
    outDrainThread.start()

    val errBAOS = new ByteArrayOutputStream()
    /* Copy process stderr to errBAOS and optionally our stderr */
    val errDrainThread = new StreamDrainThread(p.getErrorStream, errBAOS, if (teeStdOutErr) Some(java.lang.System.err) else None, "Subprocess stderr reader")
    errDrainThread.start()

    val exitValue = p.waitFor()

    outDrainThread.join(10 /*ms*/)
    errDrainThread.join(10 /*ms*/)
    assert(!outDrainThread.isAlive, "outDrainThread should be dead for "+command)
    assert(!errDrainThread.isAlive, "errDrainThread should be dead for "+command)

    val stdoutString = outBAOS.toString(charset.name)
    val stderrString = errBAOS.toString(charset.name)

    OsCommandResult(exitValue, stdoutString, stderrString)
  }

  def addSeperator[A](xs: Traversable[A], sep: A) = {
    val b = xs.companion.newBuilder[A]
    for (x <- xs) {
      b += x
      b += sep
    }
    b.result().init
  }

  def newTerminalWindowWith(commands: Seq[Seq[String]], windowTitle: String, numRows: Int, numColumns: Int): OsCommandResult = {
    DesktopType.get match {
      case DesktopType.MacOS => {
        def escapeStringForAppleScript(s: String) = s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"")
        val commandsAsString = commands.map(_.map(escapeStringForAppleScript(_)).mkString("quoted form of \"", "\" & \" \" & quoted form of \"", "\"")).mkString(" & \" ; \" & ")
        val script = s"""-- Generated AppleScript from orc.test.OsCommand.newTerminalWindowWith

tell application "Terminal"
	set newTab to do script ""
	tell newTab
		set custom title to "${escapeStringForAppleScript(windowTitle)}"
		set number of rows to ${numRows}
		set number of columns to ${numColumns}
	end tell
	do script "clear ; " & ${commandsAsString} & " ; exit" in newTab
end tell
"""
        val res = getResultFrom(Seq("osascript", "-l", "AppleScript", "-"), script)
        res
      }
      case DesktopType.Gnome => {
        def escapeStringForUnixShell(s: String) = "'" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\\'") + "'"
        val commandsAsArgs = addSeperator(commands.map(_.map(escapeStringForUnixShell(_))), Seq(" ; ")).flatten
        /* TODO: Test this guess */
        getResultFrom(Seq("gnome-terminal", s"--geometry=${numColumns}x${numRows}", "-x", "bash", "-c") ++ commandsAsArgs)
        /* TODO: Escape commands as needed */
      }
      case DesktopType.XFCE => {
        def escapeStringForUnixShell(s: String) = "'" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\"") + "'"
        val commandsAsArgs = addSeperator(commands.map(_.map(escapeStringForUnixShell(_))), Seq(" ; ")).flatten
        /* TODO: Test this guess */
        getResultFrom(Seq("xfce4-terminal", "-x", "bash", "-c", commandsAsArgs.mkString(" ")))
        /* TODO: Escape commands as needed */
      }
      case DesktopType.KDE => {
        def escapeStringForUnixShell(s: String) = "'" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\'", "\\\\\"") + "'"
        val commandsAsArgs = addSeperator(commands.map(_.map(escapeStringForUnixShell(_))), Seq(" ; ")).flatten
        /* TODO: Test this guess */
        getResultFrom(Seq("konsole", s"--geometry=${numColumns}x${numRows}", "-x", "bash", "-c") ++ commandsAsArgs)
        /* TODO: Escape commands as needed */
      }
      //case DesktopType.CDE => ???
      case DesktopType.Windows => {
        def escapeStringForWindowsCmd(s: String) = "\"" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\""
        val modeCmd = Seq("MODE", "CON:", s"COLS=${numColumns}", s"LINES=${numRows}")
        val commandsAsArgs = addSeperator(modeCmd +: commands, Seq("&&")).flatten
        /* TODO: Test this guess */
        getResultFrom(Seq("START", escapeStringForWindowsCmd(windowTitle), "%COMSPEC%", "/C") ++ commandsAsArgs)
        /* TODO: Test and fix string escaping */
      }
      case _ => throw new NotImplementedError("Don't know how to open a new shell window with this desktop environment manager")
    }
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

abstract sealed trait DesktopType
object DesktopType {
  case object MacOS extends DesktopType
  case object Gnome extends DesktopType
  case object KDE extends DesktopType
  case object CDE extends DesktopType
  case object XFCE extends DesktopType
  case object Windows extends DesktopType
  case object DontKnow extends DesktopType

  lazy val get = {
    val osName = System.getProperty("os.name")
    if (osName.startsWith("Mac OS X")) DesktopType.MacOS
    else if (osName.startsWith("macOS")) DesktopType.MacOS
    else if (osName.startsWith("Windows")) DesktopType.Windows
    else if (System.getenv("GNOME_DESKTOP_SESSION_ID") != null) DesktopType.Gnome
    else if (System.getenv("XDG_CURRENT_DESKTOP") == "XFCE") DesktopType.XFCE
    else if (System.getenv("KDE_FULL_SESSION") != null) DesktopType.KDE
    // TODO: else if (???) DesktopType.CDE
    else DesktopType.DontKnow
  }
}
