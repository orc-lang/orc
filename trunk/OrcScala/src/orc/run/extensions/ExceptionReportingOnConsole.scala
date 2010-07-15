//
// ExceptionReportingOnConsole.scala -- Scala class/trait/object ExceptionReportingOnConsole
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.OrcRuntime
import orc.error.OrcException
import orc.error.runtime.JavaException
import orc.error.runtime.TokenException
import scala.util.parsing.input.Position;
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Mix in for structured diagnostics of various Orc runtime exception types
 *
 * @author dkitchin, jthywiss
 */
trait ExceptionReportingOnConsole extends OrcRuntime {
  def caught(e: Throwable) {
    val out = Console.err
    val debugWithJavaStacktraces = false
    e match {
      case je: JavaException => {
        // Something blew during a Site call or JavaProxy call
        out.println(je.getPosition().toString() + ": " + je.getCause().getClass().getName()+": " + je.getCause().getMessage())
        out.println(je.getPosition().longString)
        printOrcBacktrace(je.getBacktrace(), out)
        printJavaBacktrace(je.getCause(), out)
      }
      case te: TokenException => {
        // Something that affects this token only
        out.println(te.getPosition().toString() + ": " + te.getClass().getName()+": " + te.getMessageOnly())
        out.println(te.getPosition().longString)
        printOrcBacktrace(te.getBacktrace(), out)
        if (debugWithJavaStacktraces) printJavaBacktrace(te, out)
      }
      case oe: OrcException => {
        // An execution-wide event
        out.println(oe.getPosition().toString() + ": " + oe.getClass().getName()+": " + oe.getMessageOnly())
        out.println(oe.getPosition().longString)
        if (debugWithJavaStacktraces) printJavaBacktrace(oe, out)
      }
      case _ => {
        // Probably a runtime engine bug
        e.printStackTrace(out)
      }
    }
  }

  private def printOrcBacktrace(trace: Array[Position], out: java.io.PrintStream) {
    trace foreach { t => out.println("\tcalled at " + t) }
  }

  private def printJavaBacktrace(e: Throwable, out: java.io.PrintStream) {
    val sw = new StringWriter()
    e.printStackTrace(new PrintWriter(sw))
    val traceBuf = sw.getBuffer()
    traceBuf.delete(0, traceBuf.indexOf("\n\tat "))
    out.print(traceBuf)
  }
}
