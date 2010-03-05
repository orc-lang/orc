//
// UncallableTypeException.java -- Java class UncallableTypeException
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

/**
 * Exception raised when the typechecker finds an uncallable
 * value in call position.
 * 
 * @author dkitchin
 */
public class UncallableTypeException extends TypeException {

	Type t;

	public UncallableTypeException(final Type t) {
		super("Type " + t + " cannot be called as a service or function.");
		this.t = t;
	}

}
