//
// Pruning.java -- Java class Pruning
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

package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Pruning extends Expression {

	Expression left;
	Expression right;
	Variable v;

	public Pruning(final Expression left, final Expression right, final Variable v) {
		this.left = left;
		this.right = right;
		this.v = v;
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new Pruning(left.subst(a, x), right.subst(a, x), v);
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new Pruning(left.subst(T, X), right.subst(T, X), v);
	}

	@Override
	public Set<Variable> vars() {

		final Set<Variable> s = left.vars();
		s.addAll(right.vars());
		s.remove(v);
		return s;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {

		final Env<Variable> newvars = vars.clone();
		newvars.add(v);

		return new orc.ast.oil.expression.Pruning(left.convert(newvars, typevars), right.convert(vars, typevars), v.name);
	}

	@Override
	public String toString() {
		return "(" + left + " <" + v + "< " + right + ")";
	}

}
