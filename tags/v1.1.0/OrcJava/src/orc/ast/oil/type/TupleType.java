//
// TupleType.java -- Java class TupleType
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
 * A type tuple: (T,...,T)
 * 
 * @author dkitchin
 */
public class TupleType extends Type {

	public List<Type> items;

	public TupleType(final List<Type> items) {
		this.items = items;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return items == null ? 0 : items.hashCode();
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
		final TupleType other = (TupleType) obj;
		if (items == null) {
			if (other.items != null) {
				return false;
			}
		} else if (!items.equals(other.items)) {
			return false;
		}
		return true;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {
		final List<orc.type.Type> newitems = new LinkedList<orc.type.Type>();

		for (final Type T : items) {
			newitems.add(T.transform(ctx));
		}

		return new orc.type.structured.TupleType(newitems);
	}

	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.TupleType(Type.marshalAll(items));
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(items.get(i));
		}
		s.append(')');

		return s.toString();
	}

}
