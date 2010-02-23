//
// CallPattern.java -- Java class CallPattern
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

import java.util.List;

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class CallPattern extends Pattern {

	public FreeVariable site;
	public Pattern p;

	// Create a call based on a string name
	public CallPattern(final String site, final List<Pattern> args) {
		this.site = new FreeVariable(site);
		this.p = Pattern.condense(args);
	}

	@Override
	public void process(final Variable fragment, final PatternSimplifier visitor) throws PatternException {

		final Variable result = new Variable();
		visitor.assign(result, new WithLocation(Pattern.unapply(site, fragment), getSourceLocation()));
		visitor.require(result);
		p.process(result, visitor);
	}

	@Override
	public String toString() {
		return site.name + p.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
