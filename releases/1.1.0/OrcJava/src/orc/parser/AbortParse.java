//
// AbortParse.java -- Java class AbortParse
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

package orc.parser;

import xtc.parser.ParseError;

/**
 * Signal an unrecoverable error during parsing. For example, a parse
 * error inside an include file is not recoverable because there is only
 * one way to parse an include file. This must be an unchecked
 * exception because Rats does not allow us to throw any checked exceptions.
 * 
 * @author quark
 */
public final class AbortParse extends RuntimeException {
	public ParseError parseError;

	public AbortParse(final String message, final ParseError parseError) {
		super(message);
		this.parseError = parseError;
	}
}
