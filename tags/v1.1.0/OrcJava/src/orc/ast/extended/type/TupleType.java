//
// TupleType.java -- Java class TupleType
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

package orc.ast.extended.type;

import java.util.LinkedList;
import java.util.List;

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

	@Override
	public orc.ast.simple.type.Type simplify() {

		final List<orc.ast.simple.type.Type> newitems = new LinkedList<orc.ast.simple.type.Type>();
		for (final Type T : items) {
			newitems.add(T.simplify());
		}

		return new orc.ast.simple.type.TupleType(newitems);
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
