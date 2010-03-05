//
// ListExpr.java -- Java class ListExpr
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

import java.util.List;

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Site;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Call;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.expression.Sequential;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;
import orc.runtime.ReverseListIterator;

public class ListExpr extends Expression {

	public List<Expression> es;

	public ListExpr(final List<Expression> es) {
		this.es = es;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		orc.ast.simple.expression.Expression rest = new Call(new Site(orc.ast.sites.Site.NIL));
		final ReverseListIterator<Expression> it = new ReverseListIterator<Expression>(es);
		while (it.hasNext()) {
			final orc.ast.simple.expression.Expression head = it.next().simplify();
			final Variable h = new Variable();
			final Variable r = new Variable();
			// rest >r> (Cons(h,r) <h< head)
			rest = new Sequential(rest, new Pruning(new Call(new Site(orc.ast.sites.Site.CONS), h, r), head, h), r);
		}
		return new WithLocation(rest, getSourceLocation());
	}

	@Override
	public String toString() {
		return "[" + join(es, ", ") + "]";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
