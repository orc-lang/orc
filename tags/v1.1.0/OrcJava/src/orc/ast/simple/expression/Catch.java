//
// Catch.java -- Java class Catch
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

public class Catch extends Expression {

	Def handlerDef;
	Expression tryBlock;

	public Catch(final Def handlerDef, final Expression tryBlock) {
		this.handlerDef = handlerDef;
		this.tryBlock = tryBlock;
	}

	//Performs the substitution [a/x]
	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new Catch(handlerDef.subst(a, x), tryBlock.subst(a, x));
	}

	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new Catch(handlerDef.subst(T, X), tryBlock.subst(T, X));
	}

	//Find the set of all unbound Vars (note: not FreeVars) in this expression.
	@Override
	public Set<Variable> vars() {
		final Set<Variable> s = handlerDef.vars();
		s.addAll(tryBlock.vars());
		return s;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Catch(handlerDef.convert(vars, typevars), tryBlock.convert(vars, typevars));
	}
}
