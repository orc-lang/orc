//
// ClassDeclaration.java -- Java class ClassDeclaration
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
import orc.ast.simple.type.ClassnameType;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.TypeVariable;

/**
 * Declaration of a class proxy. The class is given as a fully qualified Java class name.
 * It can be any Java class.
 * 
 * The declaration binds a proxy for this class to the given name. Calls to the proxy
 * behave as calls to the class's constructor.
 * 
 * @author dkitchin
 */

public class ClassDeclaration extends Declaration {

	public String varname;
	public String classname;

	public ClassDeclaration(final String v, final String c) {
		varname = v;
		classname = c;
	}

	@Override
	public orc.ast.simple.expression.Expression bindto(final orc.ast.simple.expression.Expression target) {

		final orc.ast.sites.Site s = orc.ast.sites.Site.build(orc.ast.sites.Site.JAVA, classname);
		final Argument a = new orc.ast.simple.argument.Site(s);
		orc.ast.simple.expression.Expression body = new orc.ast.simple.expression.Let(a);

		final Variable v = new Variable();
		final FreeVariable x = new FreeVariable(varname);
		body = new orc.ast.simple.expression.Pruning(target.subvar(v, x), body, v);

		final TypeVariable Y = new TypeVariable();
		final FreeTypeVariable X = new FreeTypeVariable(varname);
		final orc.ast.simple.type.Type T = new ClassnameType(classname);
		body = new orc.ast.simple.expression.DeclareType(T, Y, body);
		body = body.subvar(Y, X);

		body = new WithLocation(body, getSourceLocation());

		return body;
	}

	@Override
	public String toString() {
		return "class " + varname + " = " + classname;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
