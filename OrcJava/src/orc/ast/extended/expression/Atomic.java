//
// Atomic.java -- Java class Atomic
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

import orc.ast.extended.Visitor;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class Atomic extends Expression {

	public Expression body;

	public Atomic(final Expression body) {
		this.body = body;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		return new WithLocation(new orc.ast.simple.expression.Atomic(body.simplify()), getSourceLocation());
	}

	@Override
	public String toString() {
		return "(atomic (" + body + "))";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
