/**
 * Copyright (c) 2009, The University of Texas at Austin ("U. T. Austin")
 * All rights reserved.
 *
 * <p>You may distribute this file under the terms of the OSI Simplified BSD License,
 * as defined in the LICENSE file found in the project's top-level directory.
 */
package orc.error.compiletime;

import java.io.File;

import orc.error.SourceLocation;

/**
 * Interface to environment's message mechanism for compiler diagnostics.
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
	
	public void beginProcessing(File file);
	
	public void recordMessage(Severity severity, int code, String message, SourceLocation location, Object astNode, Throwable exception);
	//TODO: Decide on some subset of parms above
	//TODO: Declare convenience methods
	//TODO: JavaDoc
	
	public Severity getMaxSeverity();
	
	public void endProcessing(File file);
}
