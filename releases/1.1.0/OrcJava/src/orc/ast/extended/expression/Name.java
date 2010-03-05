//
// Name.java -- Java class Name
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
import orc.ast.simple.expression.WithLocation;

public class Name extends Expression {

	public String name;

	public Name(final String name) {
		this.name = name;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() {
		final orc.ast.simple.argument.Argument var = new orc.ast.simple.argument.FreeVariable(name);
		var.setSourceLocation(getSourceLocation());
		return new WithLocation(new orc.ast.simple.expression.Let(var), getSourceLocation());
	}

	@Override
	public Arg argify() {
		final orc.ast.simple.argument.Argument var = new orc.ast.simple.argument.FreeVariable(name);
		var.setSourceLocation(getSourceLocation());
		return new simpleArg(var);
	}

	@Override
	public String toString() {
		return name;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
