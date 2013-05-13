//
// CompilationException.java -- Java class CompilationException
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

package orc.error.compiletime;

import scala.util.parsing.input.NoPosition$;
import orc.error.OrcException;
import orc.error.compiletime.CompileLogger.Severity;

/**
 * Exceptions generated during Orc compilation from source to
 * portable compiled representations.
 * 
 * @author dkitchin
 */
@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public abstract class CompilationException extends OrcException {

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
		     if (this instanceof SeverityInternal) return Severity.INTERNAL;
		else if (this instanceof SeverityFatal)    return Severity.FATAL;
		else if (this instanceof SeverityError)    return Severity.ERROR;
		else if (this instanceof SeverityWarning)  return Severity.WARNING;
		else if (this instanceof SeverityNotice)   return Severity.NOTICE;
		else if (this instanceof SeverityInfo)     return Severity.INFO;
		else if (this instanceof SeverityDebug)    return Severity.DEBUG;
		else                                       return Severity.UNKNOWN;
	}

	/**
	 * @return "position: detailMessage (newline) position.longString"
	 */
	@Override
	public String getMessageAndPositon() {
		if (getPosition() != null && !(getPosition() instanceof NoPosition$)) {
			return getPosition().toString() + ": " + getLocalizedMessage() + (getPosition().longString().equals("\n^") ? "" : "\n" + getPosition().longString());
		} else {
			return getLocalizedMessage();
		}
	}
}
