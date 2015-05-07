//
// DeclareType.java -- Java class DeclareType
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

/**
 * A syntactic type declaration, and the expression to which it is scoped.
 * 
 * @author dkitchin
 */
public class DeclareType extends Expression {

	public Type type;
	public TypeVariable name;
	public Expression body;

	public DeclareType(final Type type, final TypeVariable name, final Expression body) {
		this.type = type;
		this.name = name;
		this.body = body;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {

		final orc.ast.oil.type.Type newtype = type.convert(typevars);
		final orc.ast.oil.expression.Expression newbody = body.convert(vars, typevars.extend(name));

		return new orc.ast.oil.expression.DeclareType(newtype, newbody);
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new DeclareType(type, name, body.subst(a, x));
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new DeclareType(type.subst(T, X), name, body.subst(T, X));
	}

	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

	@Override
	public String toString() {
		return "(\n" + "type " + name.name + " = " + type + "\n" + body + "\n)";
	}

}
