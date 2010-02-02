//
// Atomic.java -- Java class Atomic
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

public class Atomic extends Expression {

	Expression body;

	public Atomic(final Expression body) {
		this.body = body;
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new Atomic(body.subst(a, x));
	}

	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new Atomic(body.subst(T, X));
	}

	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Atomic(body.convert(vars, typevars));
	}

	@Override
	public String toString() {
		return "(atomic (" + body + "))";
	}

}
