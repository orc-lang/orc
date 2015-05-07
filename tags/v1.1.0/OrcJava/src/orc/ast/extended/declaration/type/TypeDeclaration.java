//
// TypeDeclaration.java -- Java class TypeDeclaration
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

package orc.ast.extended.declaration.type;

import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.expression.WithLocation;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.SiteType;
import orc.ast.simple.type.TypeVariable;

/**
 * Declaration of an external type. The type is specified as a fully qualified Java class name.
 * The class must be a subclass of orc.type.Type.
 * 
 * The declaration binds an instance of the class to the given type name.
 * 
 * @author dkitchin
 */

public class TypeDeclaration extends Declaration {

	public String varname;
	public String classname;

	public TypeDeclaration(final String varname, final String classname) {
		this.varname = varname;
		this.classname = classname;
	}

	@Override
	public orc.ast.simple.expression.Expression bindto(final orc.ast.simple.expression.Expression target) {

		orc.ast.simple.expression.Expression body = target;

		final TypeVariable Y = new TypeVariable();
		final FreeTypeVariable X = new FreeTypeVariable(varname);
		body = body.subvar(Y, X);

		final orc.ast.simple.type.Type T = new SiteType(classname);
		body = new orc.ast.simple.expression.DeclareType(T, Y, body);

		body = new WithLocation(body, getSourceLocation());

		return body;
	}

	@Override
	public String toString() {
		return "type " + varname + " = " + classname;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
