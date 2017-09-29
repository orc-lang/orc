//
// TerminalWindow.scala -- Scala objects TerminalWindow and DesktopType
// Project OrcTests
//
// Created by jthywiss on Sep 28, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

/** Create a new desktop GUI shell window ("Terminal" app) running the given
  * command sequence.
  *
  * @author jthywiss
  */
object TerminalWindow {
  def apply(commands: Seq[Seq[String]], windowTitle: String, numRows: Int, numColumns: Int): OsCommandResult = {
    def addSeperator[A](xs: Traversable[A], sep: A) = {
      val b = xs.companion.newBuilder[A]
      for (x <- xs) {
        b += x
        b += sep
      }
      b.result().init
    }

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
        val res = OsCommand.getResultFrom(Seq("osascript", "-l", "AppleScript", "-"), script)
        res
      }
      case DesktopType.Gnome => {
        def escapeStringForUnixShell(s: String) = "'" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\\'") + "'"
        val commandsAsArgs = addSeperator(commands.map(_.map(escapeStringForUnixShell(_))), Seq(" ; ")).flatten
        /* TODO: Test this guess */
        OsCommand.getResultFrom(Seq("gnome-terminal", s"--geometry=${numColumns}x${numRows}", "-x", "bash", "-c") ++ commandsAsArgs)
        /* TODO: Escape commands as needed */
      }
      case DesktopType.XFCE => {
        def escapeStringForUnixShell(s: String) = "'" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\"") + "'"
        val commandsAsArgs = addSeperator(commands.map(_.map(escapeStringForUnixShell(_))), Seq(" ; ")).flatten
        /* TODO: Test this guess */
        OsCommand.getResultFrom(Seq("xfce4-terminal", "-x", "bash", "-c", commandsAsArgs.mkString(" ")))
        /* TODO: Escape commands as needed */
      }
      case DesktopType.KDE => {
        def escapeStringForUnixShell(s: String) = "'" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\'", "\\\\\"") + "'"
        val commandsAsArgs = addSeperator(commands.map(_.map(escapeStringForUnixShell(_))), Seq(" ; ")).flatten
        /* TODO: Test this guess */
        OsCommand.getResultFrom(Seq("konsole", s"--geometry=${numColumns}x${numRows}", "-x", "bash", "-c") ++ commandsAsArgs)
        /* TODO: Escape commands as needed */
      }
      //case DesktopType.CDE => ???
      case DesktopType.Windows => {
        def escapeStringForWindowsCmd(s: String) = "\"" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\""
        val modeCmd = Seq("MODE", "CON:", s"COLS=${numColumns}", s"LINES=${numRows}")
        val commandsAsArgs = addSeperator(modeCmd +: commands, Seq("&&")).flatten
        /* TODO: Test this guess */
        OsCommand.getResultFrom(Seq("START", escapeStringForWindowsCmd(windowTitle), "%COMSPEC%", "/C") ++ commandsAsArgs)
        /* TODO: Test and fix string escaping */
      }
      case _ => throw new NotImplementedError("Don't know how to open a new shell window with this desktop environment manager")
    }
  }
}


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
