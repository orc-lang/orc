//
// TokenException.java -- Java class TokenException
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

import orc.error.Locatable;
import orc.error.SourceLocation;

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
public abstract class TokenException extends ExecutionException implements Locatable {
	private SourceLocation loc = SourceLocation.UNKNOWN;
	private SourceLocation[] backtrace = new SourceLocation[0];

	public TokenException(final String message) {
		super(message);
	}

	public TokenException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public void setBacktrace(final SourceLocation[] backtrace) {
		this.backtrace = backtrace;
	}

	public SourceLocation[] getBacktrace() {
		return backtrace;
	}

	public SourceLocation getSourceLocation() {
		return loc;
	}

	public void setSourceLocation(final SourceLocation location) {
		this.loc = location;
	}

	@Override
	public String toString() {
		return loc + ": " + super.toString();
	}
}
