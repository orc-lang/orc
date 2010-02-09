//
// HasType.java -- Java class HasType
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

import orc.ast.extended.type.AssertedType;
import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.error.compiletime.CompilationException;

public class HasType extends Expression {

	public Expression body;
	public Type type;

	public HasType(final Expression body, final Type type) {
		this.body = body;
		this.type = type;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		boolean checkable = true;

		/* If this is an asserted type, it is not checkable */
		orc.ast.simple.type.Type newType;
		if (type instanceof AssertedType) {
			final AssertedType atype = (AssertedType) type;
			newType = atype.type.simplify();
			checkable = false;
		} else {
			newType = type.simplify();
		}

		return new orc.ast.simple.expression.WithLocation(new orc.ast.simple.expression.HasType(body.simplify(), newType, checkable), getSourceLocation());
	}

	@Override
	public String toString() {
		return "(" + body + " :: " + type + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
