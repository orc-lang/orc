//
// Declare.java -- Java class Declare
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

import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.visitor.Visitor;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A declaration together with its scope in the AST.
 * 
 * @author dkitchin
 *
 */

public class Declare extends Expression {

	public Declaration d;
	public Expression e;

	public Declare(final Declaration d, final Expression e) {
		this.d = d;
		this.e = e;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		return d.bindto(e.simplify());
	}

	@Override
	public String toString() {
		return d + "\n" + e;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
