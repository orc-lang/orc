//
// JavaError.java -- Java class JavaError
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

import orc.runtime.Token;

/**
 * A container for Java-level exceptions raised while
 * processing a token. These are wrapped as Orc exceptions
 * so they can be handled by {@link Token#error(TokenException)}.
 */
public class JavaError extends TokenError {
	public JavaError(final Throwable cause) {
		super("Java exception: " + cause.toString(), cause);
	}

	@Override
	public String toString() {
		return getCause().toString();
	}
}
