//
// TuplePattern.java -- Java class TuplePattern
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

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class TuplePattern extends Pattern {

	public List<Pattern> args;

	public TuplePattern(final List<Pattern> args) {
		this.args = args;
	}

	public TuplePattern() {
		this.args = new LinkedList<Pattern>();
	}

	//	public Expression bind(Var u, Expression g) {
	//		
	//		for (int i = 0; i < args.size(); i++) {
	//			Pattern p = args.get(i);
	//			Expression ui = new Call(u, new Constant(i));
	//			g = p.bind(ui, g);
	//		}
	//		
	//		return g;
	//	}
	//
	//	public Expression match(Var u) {
	//	
	//		// lift(..., pi.match( u(i) ) ,...) 
	//		List<Expression> es = new LinkedList<Expression>();
	//		for (int i = 0; i < args.size(); i++) {
	//			Pattern p = args.get(i);
	//			Expression ui = new Call(u, new Constant(i));
	//			es.add(p.match(ui));
	//		}
	//		Expression liftExpr = Pattern.lift(es);
	//		
	//		// u.fits
	//		Expression sizeExpr = new Call(u, new Field("fits"));
	//		
	//		// u.fits(n), where n is the tuple pattern size
	//		Var s = new Var();
	//		Argument n = new Constant(args.size());
	//		Expression fitsExpr = new Where(new Call(s, n), sizeExpr, s);
	//		
	//		// if u.fits(n) then lift(...) else none()
	//		return Pattern.ifexp(fitsExpr, liftExpr, new Call(Pattern.NONE)); 
	//	}

	@Override
	public boolean strict() {
		return true;
	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {

		final Variable test = new Variable();
		visitor.assign(test, new WithLocation(Pattern.trysize(fragment, args.size()), getSourceLocation()));
		visitor.require(test);

		for (int i = 0; i < args.size(); i++) {
			final Pattern p = args.get(i);
			final Variable element = new Variable();
			visitor.assign(element, Pattern.nth(fragment, i));
			p.process(element, visitor);
		}

	}

	@Override
	public String toString() {
		return "(" + orc.ast.extended.expression.Expression.join(args, ", ") + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
