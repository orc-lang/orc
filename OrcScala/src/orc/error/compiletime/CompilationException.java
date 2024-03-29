//
// CompilationException.java -- Java class CompilationException
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime;

import orc.error.OrcException;
import orc.error.compiletime.CompileLogger.Severity;

/**
 * Exceptions generated during Orc compilation from source to portable compiled
 * representations.
 *
 * @author dkitchin
 */
public abstract class CompilationException extends OrcException {
    private static final long serialVersionUID = 3471866878600135322L;

    public CompilationException(final String message) {
        super(message);
    }

    public CompilationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CompilationException(final Throwable cause) {
        super(cause);
    }

    public Severity severity() {
        if (this instanceof SeverityInternal) {
            return Severity.INTERNAL;
        } else if (this instanceof SeverityFatal) {
            return Severity.FATAL;
        } else if (this instanceof SeverityError) {
            return Severity.ERROR;
        } else if (this instanceof SeverityWarning) {
            return Severity.WARNING;
        } else if (this instanceof SeverityNotice) {
            return Severity.NOTICE;
        } else if (this instanceof SeverityInfo) {
            return Severity.INFO;
        } else if (this instanceof SeverityDebug) {
            return Severity.DEBUG;
        } else {
            return Severity.UNKNOWN;
        }
    }

    /**
     * @return "position: detailMessage (newline) position.lineContentWithCaret"
     */
    @Override
    public String getMessageAndPositon() {
        if (getPosition() != null) {
            return getPosition().toString() + ": " + getLocalizedMessage() + (getPosition().lineContentWithCaret().equals("\n^") ? "" : "\n" + getPosition().lineContentWithCaret());
        } else {
            return getLocalizedMessage();
        }
    }
}
