//
// JavaException.java -- Java class JavaException
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

package orc.error.runtime;

import scala.util.parsing.input.NoPosition$;

/**
 * A container for Java-level exceptions raised by code
 * implementing sites. These are wrapped as Orc exceptions
 * to localize the failure to the calling token.
 * 
 * @author dkitchin
 */
@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public class JavaException extends SiteException {
	public JavaException(final Throwable cause) {
		super(cause.toString(), cause);
		setStackTrace(getCause().getStackTrace());
	}

	/**
	 * @return "position: ClassName: detailMessage (newline) position.longString"
	 */
	@Override
	public String getMessageAndPositon() {
		if (getPosition() != null && !(getPosition() instanceof NoPosition$)) {
			return getPosition().toString() + ": " + getCause().toString() + "\n" + getPosition().longString();
		} else {
			return getCause().toString();
		}
	}

	/**
	 * @return "position: ClassName: detailMessage (newline) position.longString (newline) Orc stack trace... (newline) Java stack trace..."
	 */
	@Override
	public String getMessageAndDiagnostics() {
		return getMessageAndPositon() + "\n" + getOrcStacktraceAsString() + getJavaStacktraceAsString(getCause());
	}
}
