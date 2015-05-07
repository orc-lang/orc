//
// DeclareDefs.java -- Java class DeclareDefs
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

import java.util.ArrayList;
import java.util.List;
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
 * 
 * A group of mutually recursive definitions, 
 * and the expression to which they are scoped.
 *
 * @author dkitchin
 */
public class DeclareDefs extends Expression {

	public List<Def> defs;
	public Expression body;

	public DeclareDefs(final List<Def> defs, final Expression body) {
		this.defs = defs;
		this.body = body;
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new DeclareDefs(Def.substAll(defs, a, x), body.subst(a, x));
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new DeclareDefs(Def.substAll(defs, T, X), body.subst(T, X));
	}

	@Override
	public Set<Variable> vars() {
		final Set<Variable> freeset = body.vars();

		// Standard notion of free vars
		for (final Def d : defs) {
			freeset.addAll(d.vars());
		}

		// Enforce visibility of mutual recursion
		for (final Def d : defs) {
			freeset.remove(d.name);
		}

		return freeset;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {

		final List<Variable> names = new ArrayList<Variable>();
		for (final Def d : defs) {
			names.add(d.name);
		}
		final Env<Variable> newvars = vars.extendAll(names);

		return new orc.ast.oil.expression.DeclareDefs(Def.convertAll(defs, newvars, typevars), body.convert(newvars, typevars));
	}

	@Override
	public String toString() {
		String repn = "(defs  ";
		for (final Def d : defs) {
			repn += "\n  " + d.toString();
		}
		repn += "\n)\n" + body.toString();
		return repn;
	}
}
