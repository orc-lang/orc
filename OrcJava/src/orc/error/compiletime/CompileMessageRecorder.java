//
// CompileMessageRecorder.java -- Java interface CompileMessageRecorder
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime;

import java.io.File;

import orc.ast.extended.ASTNode;
import orc.error.SourceLocation;

/**
 * Interface to environment's message mechanism for compiler diagnostics.
 * This my be writing to stderr, updating a GUI compile status window, or the like.
 * 
 * @author jthywiss
 */
public interface CompileMessageRecorder {
	public enum Severity {
		/**
		 * Severity of this message is not known.
		 */
		UNKNOWN,

		/**
		 * Severity of this message is internal, for tool debugging -- users don't care.
		 */
		DEBUG,

		/**
		 * Severity of this message is completely routine.  For example, counts of output size.
		 */
		INFO,

		/**
		 * Severity of this message is not routine, but not a problem.
		 */
		NOTICE,

		/**
		 * Severity of this message is a potential problem, but not bad enough to cause output to be disregarded -- it may still be usable. 
		 */
		WARNING,

		/**
		 * Severity of this message is a problem that is severe enough that output was discarded or should be discarded -- it is not usable.
		 */
		ERROR,

		/**
		 * Severity of this message is a problem that has caused input processing to be stopped. 
		 */
		FATAL,

		/**
		 * Severity of this message is an internal failure of the tool (not the user's fault).
		 */
		INTERNAL
	}

	/**
	 * Record that compile processing has begun for the given file.
	 * This also resets maxSeverity.
	 * Do not call for included files, only for the "main" file that the compiler was invoked on.
	 *  
	 * @param file File for which compiler processing is beginning
	 */
	public void beginProcessing(File file);

	/**
	 * Record a compile problem message.  This message is forwarded to the environment in
	 * a manner specific to the implementing class.
	 * 
	 * @param severity {@link Severity} of this message
	 * @param code integer code of this message (potentially used for filtering, is supported by the environment}
	 * @param message String localized message specifically describing problem for the complier user
	 * @param location {@link SourceLocation} where problem occurred. Indicate unknown location with a partially filled SourceLocation.
	 * @param astNode {@link ASTNode} subtree containing problem, or null if not applicable or not available.
	 * @param exception {@link Throwable} indicating problem, or null if not applicable or not available.
	 */
	public void recordMessage(Severity severity, int code, String message, SourceLocation location, Object astNode, Throwable exception);

	/**
	 * Convenience method, equivalent to <code>recordMessage(severity, code, message, location, null, exception)</code>
	 * 
	 * @param severity {@link Severity} of this message
	 * @param code integer code of this message (potentially used for filtering, is supported by the environment}
	 * @param message String localized message specifically describing problem for the complier user
	 * @param location {@link SourceLocation} where problem occurred. Indicate unknown location with a partially filled SourceLocation.
	 * @param exception {@link Throwable} indicating problem, or null if not applicable or not available.
	 */
	public void recordMessage(Severity severity, int code, String message, SourceLocation location, Throwable exception);

	/**
	 * Convenience method, equivalent to <code>recordMessage(severity, code, message, location, astNode, null)</code>
	 * 
	 * @param severity {@link Severity} of this message
	 * @param code integer code of this message (potentially used for filtering, is supported by the environment}
	 * @param message String localized message specifically describing problem for the complier user
	 * @param location {@link SourceLocation} where problem occurred. Indicate unknown location with a partially filled SourceLocation.
	 * @param astNode {@link ASTNode} subtree containing problem, or null if not applicable or not available.
	 */
	public void recordMessage(Severity severity, int code, String message, SourceLocation location, Object astNode);

	/**
	 * Convenience method, equivalent to <code>recordMessage(severity, code, message, null, null, null)</code>
	 * 
	 * @param severity {@link Severity} of this message
	 * @param code integer code of this message (potentially used for filtering, is supported by the environment}
	 * @param message String localized message specifically describing problem for the complier user
	 */
	public void recordMessage(Severity severity, int code, String message);

	/**
	 * @return maximum severity of messages recoded since {@link #beginProcessing(File)} was invoked
	 */
	public Severity getMaxSeverity();

	/**
	 * Record that compile processing is complete for the given file.
	 * 
	 * @param file File for which compiler processing is complete
	 */
	public void endProcessing(File file);
}
