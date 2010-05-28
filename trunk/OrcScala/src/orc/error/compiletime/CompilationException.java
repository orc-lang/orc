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

import orc.error.OrcException;

import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.Position;
import scala.util.parsing.input.Positional;

/**
 * Exceptions generated during Orc compilation from source to
 * portable compiled representations.
 * 
 * @author dkitchin
 */
public class CompilationException extends OrcException implements Positional {
	Position position;

	public CompilationException(final String message) {
		super(message);
	}

	public CompilationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public CompilationException(final Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
		if (position != null) {
			return "At " + position + ": " + super.getMessage();
		} else {
			return super.getMessage();
		}
	}

	public String getMessageOnly() {
		return super.getMessage();
	}

	/* (non-Javadoc)
	 * @see scala.util.parsing.input.Positional#pos()
	 */
	@Override
	public Position pos() {
		return position;
	}

	/* (non-Javadoc)
	 * @see scala.util.parsing.input.Positional#pos_$eq(scala.util.parsing.input.Position)
	 */
	@Override
	public void pos_$eq(Position newpos) {
		position = newpos;
	}

	/* (non-Javadoc)
	 * @see scala.util.parsing.input.Positional#setPos(scala.util.parsing.input.Position)
	 */
	@Override
	public Positional setPos(Position newpos) {
		if (position == null || position instanceof NoPosition$) {
			position = newpos;
		}
		return this;
	}
}
