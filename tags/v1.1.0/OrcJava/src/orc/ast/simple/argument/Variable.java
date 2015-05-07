//
// Variable.java -- Java class Variable
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

package orc.ast.simple.argument;

import java.util.Set;

import orc.env.Env;
import orc.env.SearchFailureException;
import orc.error.OrcError;

/**
 * Bound variables. Equivalence on these variables is physical (==) equality.
 * 
 * These occur in argument position. They also occur as fields in combinators
 * which bind variables.
 * 
 * @author dkitchin
 */
public class Variable extends Argument {
	/* An optional string name to use for this variable in debugging contexts. */
	public String name = null;

	@Override
	public void addFree(final Set<Variable> freeset) {
		freeset.add(this);
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(final Env<Variable> vars) {
		try {
			return new orc.ast.oil.expression.argument.Variable(vars.search(this));
		} catch (final SearchFailureException e) {
			throw new OrcError(e);
		}
	}

	@Override
	public String toString() {
		return name != null ? name : "#" + hashCode();
	}
}
