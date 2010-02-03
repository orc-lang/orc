//
// InvalidOilException.java -- Java class InvalidOilException
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.errors;

public class InvalidOilException extends Exception {
	public InvalidOilException(final String arg0) {
		super(arg0);
	}

	public InvalidOilException(final Throwable cause) {
		super(cause);
	}
}
