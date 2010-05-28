//
// TokenException.java -- Java class TokenException
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
import scala.util.parsing.input.Position;
import scala.util.parsing.input.Positional;

/**
 * A localized failure at runtime. Errors of this type cause the executing
 * token to remain silent and leave the execution, but they do not otherwise
 * disrupt the execution. Since tokens are located at specific nodes and
 * can thus be assigned a source location, a TokenException implements
 * Locatable and will typically have its source location set before the
 * exception is passed back to the engine.
 * 
 * @author dkitchin
 */
public abstract class TokenException extends ExecutionException implements Positional {
	Position position;
	private Position[] backtrace = new Position[0];

	public TokenException(final String message) {
		super(message);
	}

	public TokenException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public void setBacktrace(final Position[] backtrace) {
		this.backtrace = backtrace;
	}

	public Position[] getBacktrace() {
		return backtrace;
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
