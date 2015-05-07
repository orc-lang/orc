//
// UnrepresentableTypeException.java -- Java class UnrepresentableTypeException
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
 * Thrown when trying to marshal a generated type which cannot
 * be represented syntactically.
 * @author quark
 */
public class UnrepresentableTypeException extends TypeException {
	public UnrepresentableTypeException(final Type type) {
		super(type.toString() + " has no concrete syntax.");
	}

	public UnrepresentableTypeException() {
		super("Unrepresentable type");
	}
}
