//
// ArgumentArityException.java -- Java class ArgumentArityException
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

package orc.error.compiletime.typing;

@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public class ArgumentArityException extends TypeException {

	public int arityExpected;
	public int arityReceived;

	public ArgumentArityException(final String message) {
		super(message);
	}

	public ArgumentArityException(@SuppressWarnings("hiding") final int arityExpected, @SuppressWarnings("hiding") final int arityReceived) {
		super("Expected " + arityExpected + " arguments to call, got " + arityReceived + " arguments instead.");
		this.arityExpected = arityExpected;
		this.arityReceived = arityReceived;
	}

}
