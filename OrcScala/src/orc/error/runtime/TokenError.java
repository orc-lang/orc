//
// TokenError.java -- Java class TokenError
// Project OrcScala
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.runtime;

/**
 * A non-recoverable error at a token, which must result in halting the whole
 * engine. This extends TokenException because it simplifies error handling to
 * have a common superclass for token errors and exceptions.
 *
 * @author quark
 */

public abstract class TokenError extends TokenException {
    private static final long serialVersionUID = 5731144810559347732L;

    public TokenError(final String message) {
        super(message);
    }

    public TokenError(final String message, final Throwable cause) {
        super(message, cause);
    }
}
