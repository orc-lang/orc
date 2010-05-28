//
// UnboundVariableException.java -- Java class UnboundVariableException
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

import scala.util.parsing.input.Position;

public class UnboundVariableException extends CompilationException {
	public UnboundVariableException(final String key, final Position location) {
		this("Variable " + key + " is unbound");
		setPos(location);
	}

	public UnboundVariableException(final String message) {
		super(message);
	}
}
