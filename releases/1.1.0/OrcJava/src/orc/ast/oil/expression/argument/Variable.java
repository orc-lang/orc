//
// Variable.java -- Java class Variable
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

import java.util.Set;

import orc.ast.oil.visitor.Visitor;
import orc.env.Env;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * Bound variables, represented using deBruijn indices.
 * 
 * These occur in argument and expression position.
 * 
 * @author dkitchin
 *
 */

public class Variable extends Argument implements Comparable<Variable> {
	public int index;

	public Variable(final int index) {
		this.index = index;
	}

	@Override
	public int hashCode() {
		return index + getClass().hashCode();
	}

	@Override
	public boolean equals(final Object v) {
		if (v == null) {
			return false;
		}
		return v instanceof Variable && ((Variable) v).index == index;
	}

	public int compareTo(final Variable o) {
		return ((Integer) index).compareTo(o.index);
	}

	@Override
	public Object resolve(final Env<Object> env) {
		return resolveGeneric(env);
	}

	public <T> T resolveGeneric(final Env<T> env) {
		try {
			return env.lookup(this.index);
		} catch (final LookupFailureException e) {
			throw new OrcError(e);
		}
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		if (index >= depth) {
			indices.add(index - depth);
		}
	}

	@Override
	public String toString() {
		return "[#" + index + "]";
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {
		final Type t = ctx.lookupVar(index);

		if (t != null) {
			return t;
		} else {
			throw new TypeException("Could not infer sufficient type information about this variable. " + "It may be a recursive function lacking a return type ascription.");
		}
	}

	@Override
	public orc.ast.xml.expression.argument.Argument marshal() throws CompilationException {
		return new orc.ast.xml.expression.argument.Variable(index);
	}
}
