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

	/**
	 * Constructs an object of class ImpToOrcMessageAdapter.
	 * 
	 * @param impMessageHandler MessageHandler to be wrapped
	 */
	public ImpToOrcMessageAdapter(final IMessageHandler impMessageHandler) {
		super();
		this.impMessageHandler = impMessageHandler;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileMessageRecorder#beginProcessing(java.io.File)
	 */
	public void beginProcessing(final File file) {
		if (impMessageHandler == null) {
			return;
		}
		impMessageHandler.clearMessages();
	}

	/*
	 * (non-Javadoc)
	 * @see orc.error.CompileMessageRecorder#recordMessage(orc.error.CompileMessageRecorder.Severity, int, java.lang.String, orc.error.SourceLocation, java.lang.Object, java.lang.Throwable)
	 */
	public void recordMessage(final Severity severity, final int code, final String message, SourceLocation location, final Object astNode, final Throwable exception) {
		if (impMessageHandler == null) {
			return;
		}

		if (location == null) {
			location = SourceLocation.UNKNOWN;
		}

		final int eclipseSeverity = eclipseSeverityFromOrcSeverity(severity);

		if (impMessageHandler instanceof MarkerCreatorWithBatching) {
			try {
				((MarkerCreatorWithBatching) impMessageHandler).addMarker(eclipseSeverity, message, location.line, location.offset, location.endOffset);
			} catch (final LimitExceededException e) {
				//TODO: Do we care?
				Activator.log(e);
			}
		} else {
			impMessageHandler.handleSimpleMessage(message, location.offset, location.endOffset, location.column, location.endColumn, location.line, location.endLine);
		}
	}

	/**
	 * @param severity
	 * @return
	 */
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
