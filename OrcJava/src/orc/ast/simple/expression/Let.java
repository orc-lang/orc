//
// Let.java -- Java class Let
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.UnboundVariableException;

public class Let extends Expression {

	public List<Argument> args;

	public Let(final List<Argument> args) {
		this.args = args;
	}

	/* Special constructor for singleton */
	public Let(final Argument arg) {
		this.args = new LinkedList<Argument>();
		this.args.add(arg);
	}

	/* Special constructor for empty let */
	public Let() {
		this.args = new LinkedList<Argument>();
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new Let(Argument.substAll(args, a, x));
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return this;
	}

	@Override
	public Set<Variable> vars() {
		final Set<Variable> freeset = new HashSet<Variable>();
		for (final Argument a : args) {
			a.addFree(freeset);
		}
		return freeset;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws UnboundVariableException {
		if (args.size() == 1) {
			// If there is only one arg, use it directly as an expression
			return args.get(0).convert(vars);
		} else {
			// Otherwise, use the tuple creation site
			return new orc.ast.oil.expression.Call(new orc.ast.oil.expression.argument.Site(orc.ast.sites.Site.LET), Argument.convertAll(args, vars), new LinkedList<orc.ast.oil.type.Type>());
		}
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append("let");
		s.append('(');
		for (int i = 0; i < args.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(args.get(i));
		}
		s.append(')');

		return s.toString();
	}
}
