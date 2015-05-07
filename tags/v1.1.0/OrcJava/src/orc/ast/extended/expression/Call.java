//
// Call.java -- Java class Call
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.expression;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.expression.WithLocation;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public class Call extends Expression {

	public Expression caller;
	public List<Expression> args;
	public List<Type> typeArgs = null;

	public Call(final Expression caller, final List<Expression> args, final List<Type> typeArgs) {
		this.caller = caller;
		this.args = args;
		this.typeArgs = typeArgs;
	}

	public Call(final Expression caller, final List<Expression> args) {
		this.caller = caller;
		this.args = args;
	}

	public Call(final Expression caller, final Expression arg) {
		this(caller);
		this.args.add(arg);
	}

	public Call(final Expression caller) {
		this.caller = caller;
		this.args = new ArrayList<Expression>();
	}

	/* Alternate constructors for sites with string names, such as ops */
	public Call(final String s, final List<Expression> args) {
		this(new Name(s), args);
	}

	public Call(final String s, final Expression left, final Expression right) {
		this(s);
		this.args.add(left);
		this.args.add(right);
	}

	public Call(final String s, final Expression arg) {
		this(s);
		this.args.add(arg);
	}

	public Call(final String s) {
		this.caller = new Name(s);
		this.args = new ArrayList<Expression>();
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		final List<Argument> newargs = new LinkedList<Argument>();
		final Arg newcaller = caller.argify();

		List<orc.ast.simple.type.Type> newTypeArgs = null;
		if (typeArgs != null) {
			newTypeArgs = new LinkedList<orc.ast.simple.type.Type>();
			for (final Type t : typeArgs) {
				newTypeArgs.add(t.simplify());
			}
		}

		orc.ast.simple.expression.Expression e = new orc.ast.simple.expression.Call(newcaller.asArg(), newargs, newTypeArgs);
		e = newcaller.bind(e);

		for (final Expression r : args) {
			final Arg a = r.argify();
			newargs.add(a.asArg());
			e = a.bind(e);
		}

		final SourceLocation location = getSourceLocation();
		return location != null ? new WithLocation(e, getSourceLocation()) : e;
	}

	@Override
	public String toString() {
		return caller.toString() + "(" + join(args, ", ") + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
