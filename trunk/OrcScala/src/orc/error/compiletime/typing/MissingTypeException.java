//
// MissingTypeException.java -- Java class MissingTypeException
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

package orc.error.compiletime.typing;

/**
 * Exception raised 
 * 
 * @author dkitchin
 */
public class MissingTypeException extends TypeException {

	public MissingTypeException(final Throwable cause) {
		super(cause);
	}

	public MissingTypeException() {
		super("Type checker failed: couldn't obtain sufficient type information from a service or value.");
	}

	public MissingTypeException(final String message) {
		super(message);
	}

	public MissingTypeException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
