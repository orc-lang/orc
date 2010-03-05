//
// ArrowType.java -- Java class ArrowType
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
 * An arrow (lambda) type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author dkitchin
 */
public class ArrowType extends Type {

	public List<TypeVariable> typeParams;
	public List<Type> argTypes;
	public Type resultType;

	public ArrowType(final List<Type> argTypes, final Type resultType, final List<TypeVariable> typeParams) {
		this.typeParams = typeParams;
		this.argTypes = argTypes;
		this.resultType = resultType;
	}

	@Override
	public orc.ast.oil.type.Type convert(final Env<TypeVariable> env) throws TypeException {
		final Env<TypeVariable> newenv = env.extendAll(typeParams);
		return new orc.ast.oil.type.ArrowType(Type.convertAll(argTypes, newenv), resultType.convert(newenv), typeParams.size());
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(final Type T, final FreeTypeVariable X) {
		return new ArrowType(Type.substAll(argTypes, T, X), resultType.subst(T, X), typeParams);
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');

		s.append("lambda ");
		s.append('[');
		for (int i = 0; i < typeParams.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(typeParams.get(i));
		}
		s.append(']');
		s.append('(');
		for (int i = 0; i < argTypes.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(argTypes.get(i));
		}
		s.append(')');
		s.append(" :: ");
		s.append(resultType);

		s.append(')');

		return s.toString();
	}

}
