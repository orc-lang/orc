//
// PrintWriterCompileLogger.java -- Java class PrintWriterCompileLogger
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 19, 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime;

import java.io.PrintWriter;

import orc.ast.AST;

import scala.util.parsing.input.Position;

/**
 * A CompileMessageRecorder that writes messages to a PrintWriter (such
 * as one for stderr).
 *
 * @author jthywiss
 */
public class PrintWriterCompileLogger implements CompileLogger {
	private PrintWriter outWriter;

	private Severity maxSeverity = Severity.UNKNOWN;

	/**
	 * Constructs an object of class PrintWriterCompileLogger.
	 *
	 * @param logToWriter the Writer configuration in use.
	 */
	public PrintWriterCompileLogger(final PrintWriter logToWriter) {
		if (logToWriter == null) {
			throw new NullPointerException("Cannot construct PrintWriterCompileLogger with a null PrintWriter");
		}
		outWriter = logToWriter;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#beginProcessing(java.lang.String)
	 */
	@Override
	public void beginProcessing(final String filename) {
		if (outWriter == null) {
			throw new NullPointerException("Cannot use a options with a null stderr");
		}
		maxSeverity = Severity.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#endProcessing(java.lang.String)
	 */
	@Override
	public void endProcessing(final String filename) {
		// Nothing needed
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST, Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, Position location, final AST astNode, final Throwable exception) {

		maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;

		if (location != null) {
			outWriter.println(location.toString() + ": " + message + (exception instanceof CompilationException ? " [[OrcWiki:"+exception.getClass().getSimpleName()+"]]" : ""));
			outWriter.println(location.longString());
		} else {
			outWriter.println("<undefined position>: " + message);
		}
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position location, final Throwable exception) {
		recordMessage(severity, code, message, location, null, exception);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position location, final AST astNode) {
		recordMessage(severity, code, message, location, astNode, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message) {
		recordMessage(severity, code, message, null, null, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#getMaxSeverity()
	 */
	@Override
	public Severity getMaxSeverity() {
		return maxSeverity;
	}
}
