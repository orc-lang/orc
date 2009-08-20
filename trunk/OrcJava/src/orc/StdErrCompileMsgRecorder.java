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
import orc.error.compiletime.CompileMessageRecorder.Severity;

/**
 * 
 *
 * @author jthywiss
 */
public class StdErrCompileMsgRecorder implements CompileMessageRecorder {
	private Config config;

	private Severity maxSeverity = Severity.UNKNOWN;

	/**
	 * Constructs an object of class StdErrCompileMsgRecorder.
	 *
	 * @param config the Orc configuration in use.
	 */
	public StdErrCompileMsgRecorder(Config config) {
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
		String caret = location.getCaret();
		if (caret != null)
			config.getStderr().println(caret);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#getMaxSeverity()
	 */
	public Severity getMaxSeverity() {
		return maxSeverity;
	}

	/*
	private void otherStuffWeMayWantToTrack() {
		//TODO: do stuff here, like:

		// -------- A message sequence number --------
		// Use a private static synchronized getNextSeqNum() method

		// -------- Timestamp --------
		System.currentTimeMillis();

		// -------- Thread ID --------
		Thread.currentThread().toString();
		Thread.currentThread().getName();
		Thread.currentThread().getId();

		// -------- Caller: class name, method name, source file name, source file line number --------
		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

		// Skip down to this method invocation in the stack
		int currElemIndex = 0;
		for (; currElemIndex < stackTrace.length; currElemIndex++) {
			if (stackTrace[currElemIndex].getClass().getName() == this.getClass().getName()) {
				// Found ourselves
				break;
			}
		}
		// Now skip to an "interesting" caller 
		for (; currElemIndex < stackTrace.length; currElemIndex++) {
			if (stackTrace[currElemIndex].getClass().getName() == this.getClass().getName()) {
				// Still in this class
				continue;
			}
			// Skip everyone implementing the CompileMessageRecorder interface
			if (!stackTrace[currElemIndex].getClass().isAssignableFrom(CompileMessageRecorder.class.getClass())) {
				break;
			}
		}
		StackTraceElement caller = null;
		if (currElemIndex < stackTrace.length) {
			caller = stackTrace[currElemIndex];
		}

		if (caller != null) {
			caller.getClassName();
			caller.getMethodName();
			caller.getFileName(); // might be null
			caller.getLineNumber(); // might be < 0
		} else {
			// Didn't find a StackTraceElement meeting our needs
		}
	}
	*/

}
