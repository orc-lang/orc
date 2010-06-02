//
// ArityMismatchException.java -- Java class ArityMismatchException
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.runtime;

@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public class ArityMismatchException extends RuntimeTypeException {

	public int arityExpected;
	public int arityProvided;

	public ArityMismatchException(final String message) {
		super(message);
	}

	public ArityMismatchException(final String message, @SuppressWarnings("hiding") final int arityExpected, @SuppressWarnings("hiding") final int arityProvided) {
		super(message);
		this.arityExpected = arityExpected;
		this.arityProvided = arityProvided;
	}

	public ArityMismatchException(@SuppressWarnings("hiding") final int arityExpected, @SuppressWarnings("hiding") final int arityProvided) {
		super("Arity mismatch, expected " + arityExpected + " arguments, got " + arityProvided + " arguments.");
		this.arityExpected = arityExpected;
		this.arityProvided = arityProvided;
	}

}
