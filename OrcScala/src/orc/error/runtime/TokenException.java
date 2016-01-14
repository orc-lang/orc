//
// TokenException.java -- Java class TokenException
// Project OrcScala
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.runtime;

import scala.util.parsing.input.Position;

/**
 * A localized failure at runtime. Errors of this type cause the executing token
 * to remain silent and leave the execution, but they do not otherwise disrupt
 * the execution. Since tokens are located at specific nodes and can thus be
 * assigned a source location, a TokenException implements Locatable and will
 * typically have its source location set before the exception is passed back to
 * the engine.
 *
 * @author dkitchin
 */
public abstract class TokenException extends ExecutionException {
    private static final long serialVersionUID = -9019214813154694088L;

    public Position[] backtrace = new Position[0];

    public TokenException(final String message) {
        super(message);
    }

    public TokenException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @return 
     *         "position: ClassName: detailMessage (newline) position.longString [if available] (newline) Orc stack trace..."
     */
    @Override
    public String getMessageAndDiagnostics() {
        return getMessageAndPositon() + "\n" + getOrcStacktraceAsString();
    }

    /**
     * @return A string with a line for each stack element, in the format
     *         "\tcalled at "position
     */
    public String getOrcStacktraceAsString() {
        final StringBuilder sb = new StringBuilder();
        for (final Position t : backtrace) {
            sb.append("\tcalled at " + t + "\n");
        }
        return sb.toString();
    }

    public void setBacktrace(@SuppressWarnings("hiding") final Position[] backtrace) {
        this.backtrace = backtrace;
    }

    public Position[] getBacktrace() {
        return backtrace;
    }
}
