//
// Tracer.java -- Java class Tracer
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

package orc.trace;

/**
 * Interface for tracing an Orc execution.
 * Most of the work is done by the {@link TokenTracer}
 * returned by {@link #start()}.
 * 
 * @author quark
 */
public abstract class Tracer {
	/**
	 * Begin an execution; return the tracer for the first token.
	 */
	public abstract TokenTracer start();

	/**
	 * End an execution.
	 */
	public abstract void finish();
}
