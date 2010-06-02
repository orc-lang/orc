//
// ArgumentTypeMismatchException.java -- Java class ArgumentTypeMismatchException
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
public class ArgumentTypeMismatchException extends RuntimeTypeException {

	public int argPosition;
	public String expectedType;
	public String providedType;

	public ArgumentTypeMismatchException(final String message) {
		super(message);
	}

	public ArgumentTypeMismatchException(@SuppressWarnings("hiding") final int argPosition, @SuppressWarnings("hiding") final String expectedType, @SuppressWarnings("hiding") final String providedType) {
		super("Expected type " + expectedType + " for argument " + argPosition + ", got " + providedType + " instead");
		this.argPosition = argPosition;
		this.expectedType = expectedType;
		this.providedType = providedType;
	}

	public ArgumentTypeMismatchException(final String message, @SuppressWarnings("hiding") final int argPosition, @SuppressWarnings("hiding") final String expectedType, @SuppressWarnings("hiding") final String providedType) {
		super(message);
		this.argPosition = argPosition;
		this.expectedType = expectedType;
		this.providedType = providedType;
	}

	public ArgumentTypeMismatchException(final ClassCastException e) {
		super(e.toString());
	}

	public ArgumentTypeMismatchException(@SuppressWarnings("hiding") final int argPosition, final ClassCastException e) {
		super("For argument " + argPosition + ": " + e.toString());
	}
}
