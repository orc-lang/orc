//
// SubtypeFailureException.java -- Java class SubtypeFailureException
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

import orc.types.Type;

@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public class SubtypeFailureException extends TypeException {

	public Type S;
	public Type T;

	public SubtypeFailureException(@SuppressWarnings("hiding") final Type S, @SuppressWarnings("hiding") final Type T) {
		super("Expected type " + T + " or some subtype, found type " + S + " instead.");
		this.S = S;
		this.T = T;
	}

}
