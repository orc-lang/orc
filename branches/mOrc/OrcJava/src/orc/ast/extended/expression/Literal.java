//
// Literal.java -- Java class Literal
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
import xtc.util.Utilities;

public class Literal extends Expression {

	Object val;

	public Literal(final Object val) {
		this.val = val;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() {
		return new orc.ast.simple.expression.Let(new orc.ast.simple.argument.Constant(val));
	}

	@Override
	public Arg argify() {
		return new simpleArg(new orc.ast.simple.argument.Constant(val));
	}

	@Override
	public String toString() {
		if (val == null) {
			return String.valueOf(val);
		} else if (val instanceof String) {
			return '"' + Utilities.escape((String) val, Utilities.JAVA_ESCAPES) + '"';
		} else {
			return String.valueOf(val);
		}
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
