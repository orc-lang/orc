//
// StdErrCompileMsgRecorder.java -- Java class StdErrCompileMsgRecorder
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Aug 19, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.io.File;

import orc.error.SourceLocation;
import orc.error.compiletime.CompileMessageRecorder;

/**
 * A CompileMessageRecorder that writes messages to the stderr stream,
 * as given in an Orc Config instance.
 *
 * @author jthywiss
 */
public class StdErrCompileMsgRecorder implements CompileMessageRecorder {
	private final Config config;

	private Severity maxSeverity = Severity.UNKNOWN;

	/**
	 * Constructs an object of class StdErrCompileMsgRecorder.
	 *
	 * @param config the Orc configuration in use.
	 */
	public StdErrCompileMsgRecorder(final Config config) {
		if (config == null) {
			throw new NullPointerException("Cannot construct StdErrCompileMsgRecorder with a null config");
		}
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#beginProcessing(java.io.File)
	 */
	public void beginProcessing(final File file) {
		if (config.getStderr() == null) {
			throw new NullPointerException("Cannot use a config with a null stderr");
		}
		maxSeverity = Severity.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#endProcessing(java.io.File)
	 */
	public void endProcessing(final File file) {
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

		config.getStderr().println(location.toString() + ": " + message);
		final String caret = location.getCaret();
		if (caret != null) {
			config.getStderr().println(caret);
		}
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#recordMessage(orc.error.compiletime.CompileMessageRecorder.Severity, int, java.lang.String, orc.error.SourceLocation, java.lang.Throwable)
	 */
	public void recordMessage(Severity severity, int code, String message, SourceLocation location, Throwable exception) {
		recordMessage(severity, code, message, location, null, exception);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#recordMessage(orc.error.compiletime.CompileMessageRecorder.Severity, int, java.lang.String, orc.error.SourceLocation, java.lang.Object)
	 */
	public void recordMessage(Severity severity, int code, String message, SourceLocation location, Object astNode) {
		recordMessage(severity, code, message, location, astNode, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#recordMessage(orc.error.compiletime.CompileMessageRecorder.Severity, int, java.lang.String)
	 */
	public void recordMessage(Severity severity, int code, String message) {
		recordMessage(severity, code, message, null, null, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#getMaxSeverity()
	 */
	public Severity getMaxSeverity() {
		return maxSeverity;
	}
}
