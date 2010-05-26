//
// PatternException.java -- Java class PatternException
// Project OrcJava
//
// $Id: PatternException.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime;

public class PatternException extends CompilationException {

	public PatternException(final String message) {
		super(message);
	}

}
