//
// Field.java -- Java class Field
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

import orc.ast.extended.visitor.Visitor;

public class Field extends Expression {

	public String field;

	public Field(final String field) {
		this.field = field;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() {
		throw new Error("Field accesses can only occur in argument position");
	}

	@Override
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.argument.Field(field));
	}

	@Override
	public String toString() {
		return field;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
