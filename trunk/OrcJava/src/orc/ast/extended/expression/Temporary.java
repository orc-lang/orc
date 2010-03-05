//
// Temporary.java -- Java class Temporary
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
import orc.ast.simple.argument.Variable;

/**
 * 
 * A temporary variable, which embeds a Variable object from the simple
 * AST within the extended AST. Used to avoid the problem of generating
 * string identifiers for unnamed vars in translation steps.
 *
 * @author dkitchin
 */
public class Temporary extends Expression {

	public Variable v;

	public Temporary(final Variable v) {
		this.v = v;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() {
		return new orc.ast.simple.expression.Let(v);
	}

	@Override
	public Arg argify() {
		return new simpleArg(v);
	}

	@Override
	public String toString() {
		return "_temp" + v.hashCode();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
