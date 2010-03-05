//
// ConsPattern.java -- Java class ConsPattern
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

public class ConsPattern extends Pattern {

	public Pattern h;
	public Pattern t;

	public ConsPattern(final Pattern h, final Pattern t) {

		this.h = h;
		this.t = t;
	}

	//	public Expression bind(Var u, Expression g) {
	//	
	//		g = h.bind(new Call(Pattern.HEAD, u), g);
	//		g = t.bind(new Call(Pattern.TAIL, u), g);
	//		return g;
	//	}
	//
	//	public Expression match(Var u) {
	//				
	//		Var w = new Var();
	//		Var vh = new Var();
	//		Var vt = new Var();
	//		
	//		// Cons(vh,vt)
	//		Var z = new Var();
	//		Expression finalExpr = new Call(Pattern.CONS, vh, vt);
	//		
	//		// t.match w(1) -some(vt)-> ...
	//		Expression tailExpr = Pattern.opbind(t.match(new Call(w, new Constant(1))), vt, finalExpr);
	//		
	//		// h.match w(0) -some(vh)-> (... >u> isSome(u))
	//		Expression headExpr = Pattern.opbind(h.match(new Call(w, new Constant(0))), vh, Pattern.filter(tailExpr));
	//		
	//		// isCons(u) -some(w)-> (... >u> isSome(u))
	//		Expression topExpr = Pattern.opbind(new Call(Pattern.ISCONS, u), w, Pattern.filter(headExpr));
	//		
	//		return topExpr;
	//	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {

		final Variable pair = new Variable();
		visitor.assign(pair, new WithLocation(Pattern.trycons(fragment), getSourceLocation()));
		visitor.require(pair);

		final Variable head = new Variable();
		visitor.assign(head, Pattern.nth(pair, 0));
		h.process(head, visitor);

		final Variable tail = new Variable();
		visitor.assign(tail, Pattern.nth(pair, 1));
		t.process(tail, visitor);
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
