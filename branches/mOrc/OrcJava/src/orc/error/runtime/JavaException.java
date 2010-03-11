//
// JavaException.java -- Java class JavaException
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

package orc.error.runtime;

/**
 * A container for Java-level exceptions raised by code
 * implementing sites. These are wrapped as Orc exceptions
 * to localize the failure to the calling token.
 * 
 * @author dkitchin
 */
public class JavaException extends SiteException {
	public JavaException(final Throwable cause) {
		super(cause.toString(), cause);
	}

	@Override
	public String toString() {
		return getCause().toString();
	}
}
