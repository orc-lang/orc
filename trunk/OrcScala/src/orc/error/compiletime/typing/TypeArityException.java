//
// TypeArityException.java -- Java class TypeArityException
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

public class TypeArityException extends TypeException {

	public Integer arityExpected;
	public Integer arityReceived;

	public TypeArityException(final String message) {
		super(message);
	}

	public TypeArityException(final int arityExpected, final int arityReceived) {
		super("Expected " + arityExpected + " arguments to type instantiation, got " + arityReceived + " arguments instead.");
		this.arityExpected = arityExpected;
		this.arityReceived = arityReceived;
	}

}
