//
// ArgumentTypeMismatchException.java -- Java class ArgumentTypeMismatchException
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

public class ArgumentTypeMismatchException extends RuntimeTypeException {

	int position;
	String expectedType;
	String providedType;

	public ArgumentTypeMismatchException(final String message) {
		super(message);
	}

	public ArgumentTypeMismatchException(final int position, final String expectedType, final String providedType) {
		super("Expected type " + expectedType + " for argument " + position + ", got " + providedType + " instead");
		this.position = position;
		this.expectedType = expectedType;
		this.providedType = providedType;
	}

	public ArgumentTypeMismatchException(final String message, final int position, final String expectedType, final String providedType) {
		super(message);
		this.position = position;
		this.expectedType = expectedType;
		this.providedType = providedType;
	}

	public ArgumentTypeMismatchException(final ClassCastException e) {
		super(e.toString());
	}

	public ArgumentTypeMismatchException(final int position, final ClassCastException e) {
		super("For argument " + position + ": " + e.toString());
	}
}
