//
// UnboundVariableException.java -- Java class UnboundVariableException
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

import orc.error.SourceLocation;

public class UnboundVariableException extends CompilationException {
	public UnboundVariableException(final String key, final SourceLocation location) {
		this("Variable " + key + " is unbound");
		setSourceLocation(location);
	}

	public UnboundVariableException(final String message) {
		super(message);
	}
}
