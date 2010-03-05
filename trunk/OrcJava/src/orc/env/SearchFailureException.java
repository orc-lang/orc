//
// SearchFailureException.java -- Java class SearchFailureException
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

package orc.env;

/**
 * 
 * @author dkitchin
 */
public class SearchFailureException extends EnvException {

	public SearchFailureException(final String message) {
		super(message);
	}

	public SearchFailureException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
