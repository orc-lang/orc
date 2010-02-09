//
// TypedPattern.java -- Java class TypedPattern
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

package orc.ast.extended.pattern;

import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.error.compiletime.PatternException;

/**
 * A pattern with a type ascription.
 * 
 * WARNING: The pattern simplifier will occasionally ignore type ascriptions because they
 * are not ascribed to attachments.
 * 
 * @author dkitchin
 */
public class TypedPattern extends Pattern {

	public Pattern p;
	public Type t;

	public TypedPattern(final Pattern p, final Type t) {
		this.p = p;
		this.t = t;
	}

	@Override
	public boolean strict() {

		return p.strict();
	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {

		visitor.ascribe(fragment, t.simplify());
		p.process(fragment, visitor);
	}

	@Override
	public String toString() {
		return "(" + p + " :: " + t + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
