//
// MultiType.java -- Java class MultiType
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

package orc.sites.compatibility.type.structured;

import java.util.LinkedList;
import java.util.List;

import orc.sites.compatibility.type.Type;

/**
 * A composite type supporting ad-hoc polymorphic calls.
 * 
 * Contains a list of types; when this type is used in call position,
 * it will be typechecked using each type in the list sequentially until
 * one succeeds.
 * 
 * @author dkitchin
 */
@SuppressWarnings("hiding")
public class MultiType extends Type {

	List<Type> alts;

	public MultiType( final List<Type> alts) {
		this.alts = alts;
	}

	// binary case
	public MultiType(final Type A, final Type B) {
		this.alts = new LinkedList<Type>();
		alts.add(A);
		alts.add(B);
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');
		for (int i = 0; i < alts.size(); i++) {
			if (i > 0) {
				s.append(" & ");
			}
			s.append(alts.get(i));
		}
		s.append(')');

		return s.toString();
	}
}
