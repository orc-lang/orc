//
// UnspecifiedReturnTypeException.java -- Java class UnspecifiedReturnTypeException
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

public class UnspecifiedReturnTypeException extends InsufficientTypeInformationException {

	public UnspecifiedReturnTypeException() {
		this("Could not perform type check due to missing return type; please add a return type annotation");
	}

	public UnspecifiedReturnTypeException(final String message) {
		super(message);
	}

}
