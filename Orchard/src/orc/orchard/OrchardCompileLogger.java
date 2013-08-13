//
// OrchardCompileLogger.java -- Java class OrchardCompileLogger
// Project Orchard
//
// $Id$
//
// Created by jthywiss on Aug 26, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.List;

import orc.ast.AST;
import orc.compile.parse.OrcInputContext;
import orc.error.compiletime.CompileLogger;
import scala.util.parsing.input.Position;

/**
 * A CompileMessageRecorder that collects logged compile messages into a list
 *
 * @author jthywiss
 */
public class OrchardCompileLogger implements CompileLogger {
	/**
	 * A record of a logged compile message
	 *
	 * @author jthywiss
	 */
	public static class CompileMessage {

		public final Severity severity;
		public final int code;
		public final String message;
		public final Position position;
		public final AST astNode;
		public final Throwable exception;

		/**
		 * Constructs an object of class CompileMessage.
		 *
		 * @param severity
		 * @param code
		 * @param message
		 * @param position
		 * @param astNode
		 * @param exception
		 */
		public CompileMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode, final Throwable exception) {
			this.severity = severity;
			this.code = code;
			this.message = message;
			this.position = position;
			this.astNode = astNode;
			this.exception = exception;
		}

		public String longMessage() {
			if (position != null) {
				return position.toString() + ": " + message + "\n" + position.longString();
			} else {
				return "<undefined argPosition>: " + message;
			}
		}

	}

	private Severity maxSeverity = Severity.UNKNOWN;
	private final List<CompileMessage> msgList;

	/**
	 * Constructs an object of class OrchardCompileLogger.
	 */
	public OrchardCompileLogger(final List<CompileMessage> msgList) {
		this.msgList = msgList;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#beginProcessing(OrcInputContext)
	 */
	@Override
	public void beginProcessing(final OrcInputContext inputContext) {
		maxSeverity = Severity.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#endProcessing(OrcInputContext)
	 */
	@Override
	public void endProcessing(final OrcInputContext inputContext) {
		// Nothing needed
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#beginDependency(OrcInputContext)
	 */
	@Override
	public void beginDependency(final OrcInputContext inputContext) {
		// Nothing needed
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#endDependency(OrcInputContext)
	 */
	@Override
	public void endDependency(final OrcInputContext inputContext) {
		// Nothing needed
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST, Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position location, final AST astNode, final Throwable exception) {

		maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;

		msgList.add(new CompileMessage(severity, code, message, location, astNode, exception));
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
