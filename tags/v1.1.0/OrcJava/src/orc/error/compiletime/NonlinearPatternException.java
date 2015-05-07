//
// NonlinearPatternException.java -- Java class NonlinearPatternException
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

import orc.ast.simple.argument.FreeVariable;

public class NonlinearPatternException extends PatternException {
	public NonlinearPatternException(final FreeVariable x) {
		super("Variable " + x + " occurs more than once in a pattern");
		setSourceLocation(x.getSourceLocation());
	}
}
