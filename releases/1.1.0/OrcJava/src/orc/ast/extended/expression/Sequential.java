//
// Sequential.java -- Java class Sequential
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

import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.extended.pattern.WildcardPattern;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class Sequential extends Expression {

	public Expression left;
	public Expression right;
	public Pattern p;

	public Sequential(final Expression left, final Expression right, final Pattern p) {
		this.left = left;
		this.right = right;
		this.p = p;
	}

	public Sequential(final Expression left, final Expression right) {
		this(left, right, new WildcardPattern());
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		orc.ast.simple.expression.Expression source = left.simplify();
		orc.ast.simple.expression.Expression target = right.simplify();

		final Variable s = new Variable();
		final Variable t = new Variable();

		final PatternSimplifier pv = p.process(s);

		source = new orc.ast.simple.expression.Sequential(source, pv.filter(), s);
		target = pv.target(t, target);

		return new WithLocation(new orc.ast.simple.expression.Sequential(source, target, t), getSourceLocation());
	}

	@Override
	public String toString() {
		return "(" + left + " >" + p + "> " + right + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
