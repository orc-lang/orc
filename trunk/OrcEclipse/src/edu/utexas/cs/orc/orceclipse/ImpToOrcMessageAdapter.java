//
// ImpToOrcMessageAdapter.java -- Java class ImpToOrcMessageAdapter.java
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 15, 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import orc.ast.AST;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.CompileLogger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.imp.builder.MarkerCreatorWithBatching;
import org.eclipse.imp.builder.ProblemLimit.LimitExceededException;
import org.eclipse.imp.parser.IMessageHandler;

import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.OffsetPosition;
import scala.util.parsing.input.Position;

/**
 * Wraps IMP's IMessageHandler interface in Orc's CompileMessageRecorder
 * interface.
 * 
 * @author jthywiss
 */
public class ImpToOrcMessageAdapter implements CompileLogger {

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
	 * @see orc.error.compiletime.CompileLogger#beginProcessing(java.lang.String)
	 */
	@Override
	public void beginProcessing(final String filename) {
		impMessageHandler.clearMessages();
		maxSeverity = Severity.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String, scala.util.parsing.input.Position, orc.AST, java.lang.Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode, final Throwable exception) {
		Position locationNN = position;
		if (locationNN == null) {
			locationNN = NoPosition$.MODULE$;
		}

		maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;

		final int eclipseSeverity = eclipseSeverityFromOrcSeverity(severity);
		final String markerText = message + (exception instanceof CompilationException ? " [[OrcWiki:" + exception.getClass().getSimpleName() + "]]" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// IMP's AnnotationCreator breaks for our UNKNOWN offset values
		int safeStartOffset = 0;
		int safeEndOffset = -1;
		if (locationNN instanceof OffsetPosition && ((OffsetPosition) locationNN).offset() >= 0) {
			safeStartOffset = safeEndOffset = ((OffsetPosition) locationNN).offset();
		}

		if (impMessageHandler instanceof MarkerCreatorWithBatching) {
			final int safeLineNumber = locationNN.line() >= 1 ? locationNN.line() : -1;
			try {
				((MarkerCreatorWithBatching) impMessageHandler).addMarker(eclipseSeverity, markerText, safeLineNumber, safeStartOffset, safeEndOffset);
			} catch (final LimitExceededException e) {
				// IMP hasn't implemented problem limits yet, so it's not clear how this should be handled
				// We'll ignore for now
			}
		} else {
			impMessageHandler.handleSimpleMessage(markerText, safeStartOffset, safeEndOffset, locationNN.column(), locationNN.column(), locationNN.line(), locationNN.line());
		}
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String, scala.util.parsing.input.Position, java.lang.Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position position, final Throwable exception) {
		recordMessage(severity, code, message, position, null, exception);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String, scala.util.parsing.input.Position, orc.AST)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode) {
		recordMessage(severity, code, message, position, astNode, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String)
	 */
	@Override
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
	 * @see orc.error.compiletime.CompileLogger#getMaxSeverity()
	 */
	@Override
	public Severity getMaxSeverity() {
		return maxSeverity;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#endProcessing(java.lang.String)
	 */
	@Override
	public void endProcessing(final String filename) {
		if (impMessageHandler == null) {
			return;
		}

		if (impMessageHandler instanceof MarkerCreatorWithBatching) {
			((MarkerCreatorWithBatching) impMessageHandler).flush(null);
		}
	}

}
