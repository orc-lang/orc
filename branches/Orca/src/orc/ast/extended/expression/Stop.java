//
// Stop.java -- Java class Stop
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

public class Stop extends Expression {

	@Override
	public orc.ast.simple.expression.Expression simplify() {
		return new WithLocation(new orc.ast.simple.expression.Stop(), getSourceLocation());
	}

	@Override
	public String toString() {
		return "stop";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
