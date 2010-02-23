//
// LiteralPattern.java -- Java class LiteralPattern
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

import orc.ast.extended.expression.Literal;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class LiteralPattern extends Pattern {

	public Literal lit;

	public LiteralPattern(final Literal l) {
		this.lit = l;
	}

	//	public Expression bind(Var u, Expression g) {
	//		return g;
	//	}
	//
	//	public Expression match(Var u) {
	//		// u = L
	//		Expression test = new Call(Pattern.EQUAL, u, lit); 
	//		
	//		// some(L)
	//		Expression tc = new Call(Pattern.SOME, lit);
	//		
	//		// none()
	//		Expression fc = new Call(Pattern.NONE);
	//		
	//		// if (u=L) then some(L) else none()
	//		return Pattern.ifexp(test, tc, fc);
	//	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {
		final Variable test = new Variable();
		visitor.assign(test, new WithLocation(Pattern.compare(fragment, lit.argify().asArg()), getSourceLocation()));
		visitor.require(test);
	}

	@Override
	public String toString() {
		return String.valueOf(lit);
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
