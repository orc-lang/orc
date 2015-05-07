//
// ArrowType.java -- Java class ArrowType
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;

/**
 * An arrow (lambda) type: lambda[n](T,...,T) :: T
 * 
 * n is just a type arity, since type variables are nameless
 * in the OIL AST.
 * 
 * @author dkitchin
 */
public class ArrowType extends Type {

	public List<Type> argTypes;
	public Type resultType;
	public int typeArity;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (argTypes == null ? 0 : argTypes.hashCode());
		result = prime * result + (resultType == null ? 0 : resultType.hashCode());
		result = prime * result + typeArity;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ArrowType other = (ArrowType) obj;
		if (argTypes == null) {
			if (other.argTypes != null) {
				return false;
			}
		} else if (!argTypes.equals(other.argTypes)) {
			return false;
		}
		if (resultType == null) {
			if (other.resultType != null) {
				return false;
			}
		} else if (!resultType.equals(other.resultType)) {
			return false;
		}
		if (typeArity != other.typeArity) {
			return false;
		}
		return true;
	}

	public ArrowType(final List<Type> argTypes, final Type resultType, final int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {

		final List<orc.type.Type> newargs = new LinkedList<orc.type.Type>();
		for (final Type T : argTypes) {
			newargs.add(T.transform(ctx));
		}
		final orc.type.Type newresult = resultType.transform(ctx);

		return new orc.type.structured.ArrowType(newargs, newresult, typeArity);
	}

	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.ArrowType(Type.marshalAll(argTypes), resultType.marshal(), typeArity);
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');

		s.append("lambda ");
		s.append('[');
		for (int i = 0; i < typeArity; i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(".");
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
