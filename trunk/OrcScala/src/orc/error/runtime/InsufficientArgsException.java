//
// InsufficientArgsException.java -- Java class InsufficientArgsException
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

public class InsufficientArgsException extends RuntimeTypeException {

	public int missingArg;
	public int arityProvided;

	public InsufficientArgsException(final String message) {
		super(message);
	}

	public InsufficientArgsException(final String message, final int missingArg, final int arityProvided) {
		super(message);
		this.missingArg = missingArg;
		this.arityProvided = arityProvided;
	}

	public InsufficientArgsException(final int missingArg, final int arityProvided) {
		super("Arity mismatch, could not find argument " + missingArg + ", only got " + arityProvided + " arguments.");
		this.missingArg = missingArg;
		this.arityProvided = arityProvided;
	}

}
