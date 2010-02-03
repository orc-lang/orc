//
// TypeApplication.java -- Java class TypeApplication
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

package orc.ast.simple.type;

import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 */
public class TypeApplication extends Type {

	public Type typeOperator;
	public List<Type> params;

	public TypeApplication(final Type typeOperator, final List<Type> params) {
		this.typeOperator = typeOperator;
		this.params = params;
	}

	@Override
	public orc.ast.oil.type.Type convert(final Env<orc.ast.simple.type.TypeVariable> env) throws TypeException {
		return new orc.ast.oil.type.TypeApplication(typeOperator.convert(env), Type.convertAll(params, env));
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(final Type T, final FreeTypeVariable X) {
		return new TypeApplication(typeOperator.subst(T, X), Type.substAll(params, T, X));
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append(typeOperator);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(params.get(i));
		}
		s.append(']');

		return s.toString();
	}

}
