//
// TypeAliasDeclaration.java -- Java class TypeAliasDeclaration
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

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.expression.WithLocation;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.TypeVariable;

/**
 * Creating a new alias for an existing type.
 * 
 * @author dkitchin
 */

public class TypeAliasDeclaration extends Declaration {

	public String typename;
	public Type t;
	public List<String> formals;

	public TypeAliasDeclaration(final String typename, final Type t, final List<String> formals) {
		this.typename = typename;
		this.t = t;
		this.formals = formals;
	}

	@Override
	public orc.ast.simple.expression.Expression bindto(final orc.ast.simple.expression.Expression target) {

		orc.ast.simple.type.Type T = t.simplify();

		if (formals != null && formals.size() > 0) {
			final List<orc.ast.simple.type.TypeVariable> newFormals = new LinkedList<orc.ast.simple.type.TypeVariable>();
			for (final String formal : formals) {
				final TypeVariable Y = new TypeVariable();
				final FreeTypeVariable X = new FreeTypeVariable(formal);
				newFormals.add(Y);
				T = T.subvar(Y, X);
			}
			T = new orc.ast.simple.type.PolymorphicTypeAlias(T, newFormals);
		}

		orc.ast.simple.expression.Expression body = target;

		final TypeVariable Y = new TypeVariable();
		final FreeTypeVariable X = new FreeTypeVariable(typename);
		body = body.subvar(Y, X);
		body = new orc.ast.simple.expression.DeclareType(T, Y, body);
		body = new WithLocation(body, getSourceLocation());

		return body;
	}

	@Override
	public String toString() {
		return "type " + typename + " = " + t;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
