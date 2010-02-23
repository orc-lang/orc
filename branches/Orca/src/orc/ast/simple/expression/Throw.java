//
// Throw.java -- Java class Throw
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

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Throw extends Expression {

	Expression exception;

	public Throw(final Expression exception) {
		this.exception = exception;
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new Throw(exception.subst(a, x));
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new Throw(exception.subst(T, X));
	}

	@Override
	public Set<Variable> vars() {
		final Set s = new HashSet<Variable>();
		s.add(exception.vars());
		return s;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Throw(exception.convert(vars, typevars));
	}
}
