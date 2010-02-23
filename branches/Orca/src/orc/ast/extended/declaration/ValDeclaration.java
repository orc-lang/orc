//
// ValDeclaration.java -- Java class ValDeclaration
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

package orc.ast.extended.declaration;

import orc.ast.extended.expression.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class ValDeclaration extends Declaration {

	public Pattern p;
	public Expression e;

	public ValDeclaration(final Pattern p, final Expression e) {
		this.p = p;
		this.e = e;
	}

	@Override
	public orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) throws CompilationException {

		orc.ast.simple.expression.Expression source = e.simplify();

		final Variable s = new Variable();
		final Variable t = new Variable();

		final PatternSimplifier pv = p.process(s);

		source = new orc.ast.simple.expression.Sequential(source, pv.filter(), s);
		target = pv.target(t, target);

		return new WithLocation(new orc.ast.simple.expression.Pruning(target, source, t), getSourceLocation());
	}

	@Override
	public String toString() {
		return "val " + p + " = " + e;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
