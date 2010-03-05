//
// PolymorphicTypeAlias.java -- Java class PolymorphicTypeAlias
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
import orc.type.tycon.Variance;

/**
 * A representation of an aliased type with type parameters.
 * 
 * @author dkitchin
 */
public class PolymorphicTypeAlias extends Type {

	public Type type;
	public int typeArity;

	public PolymorphicTypeAlias(final Type type, final int typeArity) {
		this.type = type;
		this.typeArity = typeArity;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (type == null ? 0 : type.hashCode());
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
		final PolymorphicTypeAlias other = (PolymorphicTypeAlias) obj;
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		if (typeArity != other.typeArity) {
			return false;
		}
		return true;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {

		// Convert the syntactic type to a true type
		final orc.type.Type newType = type.transform(ctx);

		// Infer the variance of each type parameter
		final Variance[] V = new Variance[typeArity];
		for (int i = 0; i < V.length; i++) {
			V[i] = newType.findVariance(i);
		}

		final List<Variance> vs = new LinkedList<Variance>();
		for (final Variance v : V) {
			vs.add(0, v);
		}

		return new orc.type.tycon.PolymorphicAliasedType(newType, vs);
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.PolymorphicTypeAlias(type.marshal(), typeArity);
	}

}
