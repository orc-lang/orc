//
// NilPattern.java -- Java class NilPattern
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

package orc.ast.extended.pattern;

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class NilPattern extends Pattern {

	//	public Expression bind(Var u, Expression g) {
	//		return g;
	//	}
	//
	//	public Expression match(Var u) {
	//		
	//		return new Call(Pattern.ISNIL, u);
	//	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {

		final Variable nilp = new Variable();
		visitor.assign(nilp, new WithLocation(Pattern.trynil(fragment), getSourceLocation()));
		visitor.require(nilp);
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
