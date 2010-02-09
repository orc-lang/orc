//
// Let.java -- Java class Let
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

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class Let extends Expression {

	public List<Expression> args;

	// unary constructor
	public Let(final Expression arg) {
		this.args = new LinkedList<Expression>();
		this.args.add(arg);
	}

	public Let(final List<Expression> args) {
		this.args = args;
	}

	public Let() {
		this.args = new LinkedList<Expression>();
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		final List<Argument> newargs = new LinkedList<Argument>();
		orc.ast.simple.expression.Expression e = new orc.ast.simple.expression.Let(newargs);

		for (final Expression r : args) {
			final Arg a = r.argify();
			newargs.add(a.asArg());
			e = a.bind(e);
		}

		return new WithLocation(e, getSourceLocation());
	}

	@Override
	public String toString() {
		return "(" + join(args, ", ") + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
