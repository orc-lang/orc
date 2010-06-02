//
// DefinitionArityException.java -- Java class DefinitionArityException
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
public class DefinitionArityException extends TypeException {

	public int arityFromType;
	public int arityFromSyntax;

	public DefinitionArityException(final String message) {
		super(message);
	}

	public DefinitionArityException(@SuppressWarnings("hiding") final int arityFromType, @SuppressWarnings("hiding") final int arityFromSyntax) {
		super("Definition should have " + arityFromType + " arguments according to its type, observed " + arityFromSyntax + " arguments instead.");
		this.arityFromType = arityFromType;
		this.arityFromSyntax = arityFromSyntax;
	}

}
