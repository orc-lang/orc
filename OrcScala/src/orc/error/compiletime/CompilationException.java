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

	/**
	 * @return "position: detailMessage (newline) position.longString"
	 */
	@Override
	public String getMessageAndPositon() {
		if (getPosition() != null && !(getPosition() instanceof NoPosition$)) {
			return getPosition().toString() + ": " + getLocalizedMessage() + "\n" + getPosition().longString();
		} else {
			return getLocalizedMessage();
		}
	}
}
