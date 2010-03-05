//
// FreeVariable.java -- Java class FreeVariable
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.simple.argument;

import orc.env.Env;
import orc.error.compiletime.UnboundVariableException;

/**
 * Free variables. All such variables embed a String key.
 * Equivalence on these variables is equality of the embedded string.
 * 
 * Like normal Variables, these occur in argument position. 
 * 
 * The subst method on simplified expressions can only substitute for
 * a free variable.
 * 
 * @author dkitchin
 */
public class FreeVariable extends Argument implements Comparable<FreeVariable> {
	public String name;

	public FreeVariable(final String key) {
		this.name = key;
	}

	public int compareTo(final FreeVariable f) {
		final String s = this.name;
		final String t = f.name;
		return s.compareTo(t);
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof FreeVariable) {
			return this.compareTo((FreeVariable) o) == 0;
		} else {
			return this.equals(o);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name == null ? 0 : name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(final Env<Variable> vars) throws UnboundVariableException {
		throw new UnboundVariableException(name, getSourceLocation());
	}
}
