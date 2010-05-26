//
// ArityMismatchException.java -- Java class ArityMismatchException
// Project OrcJava
//
// $Id: ArityMismatchException.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.runtime;

public class ArityMismatchException extends RuntimeTypeException {

	public int arityExpected;
	public int arityProvided;

	public ArityMismatchException(final String message) {
		super(message);
	}

	public ArityMismatchException(final String message, final int arityExpected, final int arityProvided) {
		super(message);
		this.arityExpected = arityExpected;
		this.arityProvided = arityProvided;
	}

	public ArityMismatchException(final int arityExpected, final int arityProvided) {
		super("Arity mismatch, expected " + arityExpected + " arguments, got " + arityProvided + " arguments.");
		this.arityExpected = arityExpected;
		this.arityProvided = arityProvided;
	}

}
