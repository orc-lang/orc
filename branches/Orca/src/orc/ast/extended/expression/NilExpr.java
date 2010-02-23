//
// NilExpr.java -- Java class NilExpr
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

public class NilExpr extends Expression {

	@Override
	public orc.ast.simple.expression.Expression simplify() {
		return new orc.ast.simple.expression.Call(new Site(orc.ast.sites.Site.NIL));
	}

	@Override
	public String toString() {
		return "[]";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
