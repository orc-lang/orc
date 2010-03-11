//
// Argument.java -- Java class Argument
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

package orc.ast.oil.expression.argument;

import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.Expression;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.Token;
import orc.runtime.values.Value;

public abstract class Argument extends Expression {
	public abstract Object resolve(Env<Object> env);

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	abstract public int hashCode();

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	abstract public boolean equals(Object obj);

	@Override
	public abstract orc.ast.xml.expression.argument.Argument marshal() throws CompilationException;

	/* Reduce an argument list to a field name if that arg list
	 * is a singleton list of a field.
	 * Otherwise, return null.
	 */
	public static String asField(final List<Argument> args) {
		if (args.size() == 1 && args.get(0) instanceof Field) {
			final Field f = (Field) args.get(0);
			return f.key;
		} else {
			return null;
		}
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		// By default, do nothing.
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		// No children
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		final Object v = Value.forceArg(t.lookup(this), t);

		if (v != Value.futureNotReady) {
			leave(t.setResult(v));
		}
	}
}
