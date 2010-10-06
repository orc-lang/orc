//
// TypeException.java -- Java class TypeException
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

package orc.error.compiletime.typing;

import scala.util.parsing.input.Position;
import orc.error.compiletime.CompilationException;

@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public abstract class TypeException extends CompilationException {

	public TypeException(final String message) {
		super(message);
	}

	public TypeException(final Throwable cause) {
		super(cause);
	}

	public TypeException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public TypeException(final String message, final Position location) {
		super(message);
		setPosition(location);
	}

}
