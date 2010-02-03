//
// TypeApplication.java -- Java class TypeApplication
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

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;

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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (params == null ? 0 : params.hashCode());
		result = prime * result + (typeOperator == null ? 0 : typeOperator.hashCode());
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
		final TypeApplication other = (TypeApplication) obj;
		if (params == null) {
			if (other.params != null) {
				return false;
			}
		} else if (!params.equals(other.params)) {
			return false;
		}
		if (typeOperator == null) {
			if (other.typeOperator != null) {
				return false;
			}
		} else if (!typeOperator.equals(other.typeOperator)) {
			return false;
		}
		return true;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {
		return new orc.type.TypeApplication(typeOperator.transform(ctx), Type.transformAll(params, ctx));
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

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.TypeApplication(typeOperator.marshal(), Type.marshalAll(params));
	}

}
