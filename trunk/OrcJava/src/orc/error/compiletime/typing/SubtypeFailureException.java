//
// SubtypeFailureException.java -- Java class SubtypeFailureException
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

import orc.type.Type;

public class SubtypeFailureException extends TypeException {

	Type S;
	Type T;

	public SubtypeFailureException(final Type S, final Type T) {
		super("Expected type " + T + " or some subtype, found type " + S + " instead.");
		this.S = S;
		this.T = T;
	}

}
