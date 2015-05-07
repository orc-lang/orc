//
// Datatype.java -- Java class Datatype
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
import orc.type.tycon.DatatypeTycon;
import orc.type.tycon.Variance;

/**
 * A type encompassing all of the information associated with a datatype.
 * 
 * @author dkitchin
 */
public class Datatype extends Type {

	public List<List<Type>> members;
	public int typeArity;
	String name;

	public Datatype(final List<List<Type>> members, final int typeArity, final String name) {
		this.members = members;
		this.typeArity = typeArity;
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (members == null ? 0 : members.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
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
		final Datatype other = (Datatype) obj;
		if (members == null) {
			if (other.members != null) {
				return false;
			}
		} else if (!members.equals(other.members)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (typeArity != other.typeArity) {
			return false;
		}
		return true;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {

		// We use this array to infer the variance of each type parameter
		final Variance[] V = new Variance[typeArity];
		for (int i = 0; i < V.length; i++) {
			V[i] = Variance.CONSTANT;
		}

		/* Reduce each constructor to a list of its argument types.
		 * The constructor names are used separately in the dynamic
		 * semantics to give a string representation for the constructed
		 * values.
		 */
		final List<List<orc.type.Type>> cs = new LinkedList<List<orc.type.Type>>();
		for (final List<Type> con : members) {
			final List<orc.type.Type> ts = new LinkedList<orc.type.Type>();
			for (final Type t : con) {
				// Convert the syntactic type to a true type
				final orc.type.Type newT = t.transform(ctx);
				// Add it as an entry for the new constructor
				ts.add(newT);
				// Infer the variance of each type parameter it uses;
				// add that information to the array V.
				for (int i = 0; i < V.length; i++) {
					V[i] = V[i].and(newT.findVariance(i));
				}
			}
			cs.add(ts);
		}

		final List<Variance> vs = new LinkedList<Variance>();
		for (final Variance v : V) {
			vs.add(0, v);
		}

		return new DatatypeTycon(name, vs, cs, this);
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {

		final orc.ast.xml.type.Type[][] cs = new orc.ast.xml.type.Type[members.size()][0];
		int i = 0;
		for (final List<Type> ts : members) {
			cs[i++] = Type.marshalAll(ts);
		}

		return new orc.ast.xml.type.Datatype(name, cs, typeArity);
	}

}
