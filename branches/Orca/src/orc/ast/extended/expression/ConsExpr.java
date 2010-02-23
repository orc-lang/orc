//
// ConsExpr.java -- Java class ConsExpr
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

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Site;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class ConsExpr extends Expression {

	public Expression h;
	public Expression t;

	public ConsExpr(final Expression h, final Expression t) {
		this.h = h;
		this.t = t;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		final Variable vh = new Variable();
		final Variable vt = new Variable();

		orc.ast.simple.expression.Expression body = new orc.ast.simple.expression.Call(new Site(orc.ast.sites.Site.CONS), vh, vt);

		body = new orc.ast.simple.expression.Pruning(body, h.simplify(), vh);
		body = new orc.ast.simple.expression.Pruning(body, t.simplify(), vt);

		return new WithLocation(body, getSourceLocation());
	}

	@Override
	public String toString() {
		return "(" + h + ":" + t + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
