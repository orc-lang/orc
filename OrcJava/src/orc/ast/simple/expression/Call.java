//
// Call.java -- Java class Call
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
import orc.error.compiletime.CompilationException;

public class Call extends Expression {

	public Argument callee;
	public List<Argument> args;
	public List<Type> typeArgs;

	public Call(final Argument callee, final List<Argument> args, final List<Type> typeArgs) {
		this.callee = callee;
		this.args = args;
		this.typeArgs = typeArgs;
	}

	public Call(final Argument callee, final List<Argument> args) {
		this.callee = callee;
		this.args = args;
	}

	/* Binary call constructor */
	public Call(final Argument callee, final Argument arga, final Argument argb) {
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arga);
		this.args.add(argb);
	}

	/* Unary call constructor */
	public Call(final Argument callee, final Argument arg) {
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arg);
	}

	/* Nullary call constructor */
	public Call(final Argument callee) {
		this.callee = callee;
		this.args = new LinkedList<Argument>();
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new Call(callee.subst(a, x), Argument.substAll(args, a, x), typeArgs);
	}

	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new Call(callee, args, Type.substAll(typeArgs, T, X));
	}

	@Override
	public Set<Variable> vars() {
		final Set<Variable> freeset = new HashSet<Variable>();
		callee.addFree(freeset);
		for (final Argument a : args) {
			a.addFree(freeset);
		}
		return freeset;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Call(callee.convert(vars), Argument.convertAll(args, vars), Type.convertAll(typeArgs, typevars));
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append(callee);
		if (typeArgs != null) {
			s.append('[');
			for (int i = 0; i < typeArgs.size(); i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(typeArgs.get(i));
			}
			s.append(']');
		}
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
