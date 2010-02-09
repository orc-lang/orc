//
// AsPattern.java -- Java class AsPattern
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
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.error.compiletime.PatternException;

public class AsPattern extends Pattern {

	public Pattern p;
	public FreeVariable x;

	public AsPattern(final Pattern p, final String s) {
		this.p = p;
		this.x = new FreeVariable(s);
	}

	//	public Expression bind(Var u, Expression g) {
	//		
	//		Expression h = g.subst(u, x);
	//		return p.bind(u,h);
	//	}
	//
	//	public Expression match(Var u) {
	//		
	//		return p.match(u);
	//	}

	@Override
	public boolean strict() {

		return p.strict();
	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {

		visitor.subst(fragment, x);
		p.process(fragment, visitor);
	}

	@Override
	public String toString() {
		return "(" + p + " as " + x.name + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
