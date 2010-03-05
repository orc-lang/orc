//
// CompilationException.java -- Java class CompilationException
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

package orc.error.compiletime;

import orc.error.Locatable;
import orc.error.OrcException;
import orc.error.SourceLocation;

/**
 * Exceptions generated during Orc compilation from source to
 * portable compiled representations.
 * 
 * @author dkitchin
 */
public class CompilationException extends OrcException implements Locatable {
	protected SourceLocation location;

	public CompilationException(final String message) {
		super(message);
	}

	public CompilationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public CompilationException(final Throwable cause) {
		super(cause);
	}

	public void setSourceLocation(final SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}

	@Override
	public String getMessage() {
		if (location != null) {
			return "At " + location + ": " + super.getMessage();
		} else {
			return super.getMessage();
		}
	}

	public String getMessageOnly() {
		return super.getMessage();
	}
}
