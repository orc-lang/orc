//
// PrintWriterCompileLogger.java -- Java class PrintWriterCompileLogger
// Project OrcJava
//
// $Id: PrintWriterCompileLogger.java 1502 2010-02-03 06:25:53Z jthywissen $
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

import orc.error.SourceLocation;

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
	 * @see orc.error.compiletime.CompileMessageRecorder#beginProcessing(java.io.File)
	 */
	public void beginProcessing(final String filename) {
		if (outWriter == null) {
			throw new NullPointerException("Cannot use a options with a null stderr");
		}
		maxSeverity = Severity.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#endProcessing(java.io.File)
	 */
	public void endProcessing(final String filename) {
		// Nothing needed
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#recordMessage(orc.error.compiletime.CompileMessageRecorder.Severity, int, java.lang.String, orc.error.SourceLocation, java.lang.Object, java.lang.Throwable)
	 */
	public void recordMessage(final Severity severity, final int code, final String message, SourceLocation location, final Object astNode, final Throwable exception) {
		if (location == null) {
			location = SourceLocation.UNKNOWN;
		}

		maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;

		outWriter.println(location.toString() + ": " + message);
		final String caret = location.getCaret();
		if (caret != null) {
			outWriter.println(caret);
		}
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#recordMessage(orc.error.compiletime.CompileMessageRecorder.Severity, int, java.lang.String, orc.error.SourceLocation, java.lang.Throwable)
	 */
	public void recordMessage(final Severity severity, final int code, final String message, final SourceLocation location, final Throwable exception) {
		recordMessage(severity, code, message, location, null, exception);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#recordMessage(orc.error.compiletime.CompileMessageRecorder.Severity, int, java.lang.String, orc.error.SourceLocation, java.lang.Object)
	 */
	public void recordMessage(final Severity severity, final int code, final String message, final SourceLocation location, final Object astNode) {
		recordMessage(severity, code, message, location, astNode, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#recordMessage(orc.error.compiletime.CompileMessageRecorder.Severity, int, java.lang.String)
	 */
	public void recordMessage(final Severity severity, final int code, final String message) {
		recordMessage(severity, code, message, null, null, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#getMaxSeverity()
	 */
	public Severity getMaxSeverity() {
		return maxSeverity;
	}
}
