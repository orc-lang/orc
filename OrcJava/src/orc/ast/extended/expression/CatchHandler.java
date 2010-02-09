//
// CatchHandler.java -- Java class CatchHandler
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

import java.util.List;

import orc.ast.extended.ASTNode;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.visitor.Visitor;
import orc.error.SourceLocation;

public class CatchHandler implements ASTNode {
	private SourceLocation location;

	public List<Pattern> catchPattern;
	public Expression body;

	public CatchHandler(final List<Pattern> formals, final Expression body) {
		this.catchPattern = formals;
		this.body = body;
	}

	public void setSourceLocation(final SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}

	@Override
	public String toString() {
		return "catch(" + catchPattern.toString() + ")" + body.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
