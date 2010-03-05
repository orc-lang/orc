//
// Term.java -- Java interface Term
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace;

import java.io.IOException;
import java.io.Writer;

import orc.trace.events.Event;
import orc.trace.values.Value;

/**
 * Terms include {@link Value}s, {@link Event}s, and Patterns. Patterns are
 * the only terms that can include variables. The separation is necessary to
 * enforce constraints on values (for example, values can be serialized in a
 * trace file but pattern variables cannot), and also because it makes the
 * binary-dispatch nature of unification more straightforward to implement.
 * However it does lead to some duplication of boilerplate code and class
 * hierarchies.
 * 
 * @author quark
 */
public interface Term {
	/**
	 * Pretty-print the term to out. If the term is multiple lines, each newline
	 * should be followed by at least indent tabs. The value should not begin or
	 * end with a newline.
	 * @see Terms#indent(Writer, int)
	 */
	public void prettyPrint(Writer out, int indent) throws IOException;
}
