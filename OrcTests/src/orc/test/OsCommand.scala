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

import java.io.ByteArrayOutputStream
import java.nio.charset.{ Charset, StandardCharsets }

import scala.collection.JavaConverters.seqAsJavaListConverter

/** Utility methods to invoke commands of the underlying OS.
  *
  * @author jthywiss
  */
object OsCommand {

  def getResultFrom(command: Seq[String], stdin: String = "", charset: Charset = StandardCharsets.UTF_8) = {
    val p = new ProcessBuilder(command.asJava).start()

    val stdinStream = p.getOutputStream /* sic. yes, confusing naming */
    stdinStream.write(stdin.getBytes(charset.name))
    stdinStream.close()

    val exitValue = p.waitFor()

    val buff = new Array[Byte](8192)
    var bytesRead = 0

    val stdoutStream = p.getInputStream /* sic. yes, confusing naming */
    val outBAOS = new ByteArrayOutputStream()
    do {
      bytesRead = stdoutStream.read(buff)
      if (bytesRead > 0) {
        outBAOS.write(buff, 0, bytesRead)
      }
    } while (bytesRead > 0)
    val stdoutString = outBAOS.toString(charset.name)

    val stderrStream = p.getErrorStream
    val errBAOS = new ByteArrayOutputStream()
    do {
      bytesRead = stderrStream.read(buff)
      if (bytesRead > 0) {
        errBAOS.write(buff, 0, bytesRead)
      }
    } while (bytesRead > 0)
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

  def newTerminalWindowWith(commands: Seq[Seq[String]], windowTitle: String, numRows: Int, numColumns: Int) {
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
      }
      case DesktopType.Gnome => {
        def escapeStringForUnixShell(s: String) = "'" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\\'") + "'"
        val commandsAsArgs = addSeperator(commands.map(_.map(escapeStringForUnixShell(_))), Seq(" ; ")).flatten
        /* TODO: Test this guess */
        getResultFrom(Seq("gnome-terminal", "-t", escapeStringForUnixShell(windowTitle), s"--geometry=${numColumns}x${numRows}", "-x") ++ commandsAsArgs)
        /* TODO: Escape commands as needed */
      }
      //case DesktopType.KDE => ???
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

case class OsCommandResult(val exitValue: Int, val stdout: String, val stderr: String)

abstract sealed trait DesktopType
object DesktopType {
  case object MacOS extends DesktopType
  case object Gnome extends DesktopType
  case object KDE extends DesktopType
  case object CDE extends DesktopType
  case object Windows extends DesktopType
  case object DontKnow extends DesktopType

  lazy val get = {
    val osName = System.getProperty("os.name")
    if (osName.startsWith("Mac OS X")) DesktopType.MacOS
    else if (osName.startsWith("macOS")) DesktopType.MacOS
    else if (osName.startsWith("Windows")) DesktopType.Windows
    else if (System.getenv("GNOME_DESKTOP_SESSION_ID") != null) DesktopType.Gnome
    else if (System.getenv("KDE_FULL_SESSION") != null) DesktopType.KDE
    // TODO: else if (???) DesktopType.CDE
    else DesktopType.DontKnow
  }
}
