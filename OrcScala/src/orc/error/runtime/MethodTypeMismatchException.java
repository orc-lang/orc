//
// MethodTypeMismatchException.java -- Java class MethodTypeMismatchException
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
public class MethodTypeMismatchException extends RuntimeTypeException {

	public String methodName;

	public MethodTypeMismatchException(@SuppressWarnings("hiding") final String methodName) {
		super("Argument types did not match any implementation for method '" + methodName + "'.");
		this.methodName = methodName;
	}

}
