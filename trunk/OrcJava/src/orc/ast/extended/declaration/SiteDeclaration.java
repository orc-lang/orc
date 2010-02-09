//
// SiteDeclaration.java -- Java class SiteDeclaration
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

package orc.ast.extended.declaration;

import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;

/**
 * Declaration of a site. The site is specified as a fully qualified Java class name.
 * The class must be a subclass of Site.
 * 
 * The declaration binds an instance of the class to the given name.
 * 
 * @author dkitchin
 */

public class SiteDeclaration extends Declaration {

	public String varname;
	public String classname;

	public SiteDeclaration(final String v, final String c) {
		varname = v;
		classname = c;
	}

	@Override
	public orc.ast.simple.expression.Expression bindto(final orc.ast.simple.expression.Expression target) {

		final FreeVariable x = new FreeVariable(varname);
		final Variable v = new Variable();

		final orc.ast.sites.Site s = orc.ast.sites.Site.build(orc.ast.sites.Site.ORC, classname);
		final Argument a = new orc.ast.simple.argument.Site(s);

		return new WithLocation(new orc.ast.simple.expression.Pruning(target.subvar(v, x), new orc.ast.simple.expression.Let(a), v), getSourceLocation());

		/*
		orc.ast.simple.arg.Argument a;
		orc.ast.simple.arg.NamedVar x;
		
		a = new orc.ast.simple.arg.Site(orc.ast.sites.Site.build(orc.ast.sites.Site.ORC, classname));
		x = new orc.ast.simple.arg.NamedVar(varname);
		return target.subst(a,x);
		*/
	}

	@Override
	public String toString() {
		return "site " + varname + " = " + classname;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
