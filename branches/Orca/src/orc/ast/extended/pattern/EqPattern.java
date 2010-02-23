//
// EqPattern.java -- Java class EqPattern
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
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class EqPattern extends Pattern {

	public FreeVariable x;

	public EqPattern(final String s) {
		x = new FreeVariable(s);
	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {
		final Variable test = new Variable();
		visitor.assign(test, new WithLocation(Pattern.compare(fragment, x), getSourceLocation()));
		visitor.require(test);
	}

	@Override
	public String toString() {
		return x.name.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
