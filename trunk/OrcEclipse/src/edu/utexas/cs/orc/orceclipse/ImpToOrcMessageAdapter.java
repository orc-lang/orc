//
// ImpToOrcMessageAdapter.java -- Java class ImpToOrcMessageAdapter.java
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 15, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.io.File;

import orc.error.SourceLocation;
import orc.error.compiletime.CompileMessageRecorder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.imp.builder.MarkerCreatorWithBatching;
import org.eclipse.imp.builder.ProblemLimit.LimitExceededException;
import org.eclipse.imp.parser.IMessageHandler;

/**
 * Wraps IMP's IMessageHandler interface in Orc's CompileMessageRecorder
 * interface.
 * 
 * @author jthywiss
 */
public class ImpToOrcMessageAdapter implements CompileMessageRecorder {

	private final IMessageHandler impMessageHandler;

	private Severity maxSeverity = Severity.UNKNOWN;

	/**
	 * Constructs an object of class ImpToOrcMessageAdapter.
	 * 
	 * @param impMessageHandler MessageHandler to be wrapped
	 */
	public ImpToOrcMessageAdapter(final IMessageHandler impMessageHandler) {
		super();
		if (impMessageHandler == null) {
			throw new NullPointerException("Cannot instantiate ImpToOrcMessageAdapter with a null impMessageHandler"); //$NON-NLS-1$
		}
		this.impMessageHandler = impMessageHandler;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#beginProcessing(java.io.File)
	 */
	public void beginProcessing(final File file) {
		impMessageHandler.clearMessages();
		maxSeverity = Severity.UNKNOWN;
	}

	/*
	 * (non-Javadoc)
	 * @see orc.error.CompileMessageRecorder#recordMessage(orc.error.CompileMessageRecorder.Severity, int, java.lang.String, orc.error.SourceLocation, java.lang.Object, java.lang.Throwable)
	 */
	public void recordMessage(final Severity severity, final int code, final String message, final SourceLocation location, final Object astNode, final Throwable exception) {
		SourceLocation locationNN = location;
		if (locationNN == null) {
			locationNN = SourceLocation.UNKNOWN;
		}

		maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;

		final int eclipseSeverity = eclipseSeverityFromOrcSeverity(severity);

		if (impMessageHandler instanceof MarkerCreatorWithBatching) {
			final int safeLineNumber = locationNN.line >= 1 ? locationNN.line : -1;
			try {
				((MarkerCreatorWithBatching) impMessageHandler).addMarker(eclipseSeverity, message, safeLineNumber, locationNN.offset, locationNN.endOffset);
			} catch (final LimitExceededException e) {
				// Shouldn't happen, since we don't use problem limits
				Activator.log(e);
			}
		} else {
			// AnnotationCreator breaks for our UNKNOWN offset values
			final int safeStartOffset = locationNN.offset >= 0 ? locationNN.endOffset : 0;
			final int safeEndOffset = locationNN.endOffset >= 0 ? locationNN.endOffset : -1;
			impMessageHandler.handleSimpleMessage(message, safeStartOffset, safeEndOffset, locationNN.column, locationNN.endColumn, locationNN.line, locationNN.endLine);
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

	static protected int eclipseSeverityFromOrcSeverity(final Severity severity) {
		int eclipseSeverity = IMarker.SEVERITY_ERROR; // Default
		switch (severity) {
		case DEBUG:
		case INFO:
		case NOTICE:
			eclipseSeverity = IMarker.SEVERITY_INFO;
			break;
		case WARNING:
			eclipseSeverity = IMarker.SEVERITY_WARNING;
			break;
		case ERROR:
		case FATAL:
		case INTERNAL:
			eclipseSeverity = IMarker.SEVERITY_ERROR;
			break;
		default:
			// Leave default value
		}
		return eclipseSeverity;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#getMaxSeverity()
	 */
	public Severity getMaxSeverity() {
		return maxSeverity;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#endProcessing(java.io.File)
	 */
	public void endProcessing(final File file) {
		if (impMessageHandler == null) {
			return;
		}

		if (impMessageHandler instanceof MarkerCreatorWithBatching) {
			((MarkerCreatorWithBatching) impMessageHandler).flush(null);
		}
	}

}
