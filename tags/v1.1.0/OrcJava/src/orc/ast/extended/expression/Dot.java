//
// Dot.java -- Java class Dot
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
import orc.error.compiletime.CompilationException;

/**
 * A dot expression (e.g "C.put(4)"). 
 *     
 * This is interpreted as a chain of calls:
 * 
 *     C(`put`)(4)
 *     
 * where `put` is a special Field object.
 * 
 * @author dkitchin
 */

public class Dot extends Expression {

	public Expression target;
	public String field;

	public Dot(final Expression target, final String field) {
		this.target = target;
		this.field = field;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		final Call call = new Call(target, new Field(field));
		call.setSourceLocation(getSourceLocation());
		return call.simplify();
	}

	@Override
	public String toString() {
		return target + "." + field;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
