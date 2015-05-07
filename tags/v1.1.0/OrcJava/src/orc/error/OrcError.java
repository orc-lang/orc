//
// OrcError.java -- Java class OrcError
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

package orc.error;

/**
 * Error conditions that should never occur. The occurrence of such
 * an error at runtime indicates the violation of some language
 * invariant. In general this can substitute for AssertionError.
 * 
 * @author dkitchin
 */
public class OrcError extends Error {

	public OrcError(final String message) {
		super(message);
	}

	public OrcError(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

	public OrcError(final Throwable arg0) {
		super(arg0);
	}

}
