//
// WildcardPattern.java -- Java class WildcardPattern
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

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;

public class WildcardPattern extends Pattern {
	@Override
	public boolean strict() {
		return false;
	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) {
		// Do nothing.
	}

	@Override
	public String toString() {
		return "_";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
