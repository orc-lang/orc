//
// ExecutionException.java -- Java class ExecutionException
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

package orc.error.runtime;

import orc.error.OrcException;

/**
 * Exceptions generated while executing a compiled graph.
 * 
 * @author dkitchin
 */
public class ExecutionException extends OrcException {

	public ExecutionException(final String message) {
		super(message);
	}

	public ExecutionException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
