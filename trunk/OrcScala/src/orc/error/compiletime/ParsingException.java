//
// ParsingException.java -- Java class ParsingException
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

/**
 * Problem parsing the text of an Orc program. Mostly this
 * is a wrapper around the exceptions thrown by whatever
 * parsing library we use.
 * 
 * @author quark
 */
public class ParsingException extends CompilationException {
	public ParsingException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ParsingException(final String message) {
		super(message);
	}
}
