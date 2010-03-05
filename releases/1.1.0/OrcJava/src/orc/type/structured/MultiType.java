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

package orc.type.structured;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.MultiTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * A composite type supporting ad-hoc polymorphic calls.
 * 
 * Contains a list of types; when this type is used in call position,
 * it will be typechecked using each type in the list sequentially until
 * one succeeds.
 * 
 * @author dkitchin
 */
public class MultiType extends Type {

	List<Type> alts;

	public MultiType(final List<Type> alts) {
		this.alts = alts;
	}

	// binary case
	public MultiType(final Type A, final Type B) {
		this.alts = new LinkedList<Type>();
		alts.add(A);
		alts.add(B);
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {

		for (final Type alt : alts) {
			if (alt.subtype(that)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {

		MultiTypeException exn = new MultiTypeException();
		
		for (final Type alt : alts) {
			try {
				return alt.call(ctx, args, typeActuals);
			} catch (final TypeException e) {
				/* This alternative failed.
				 * Record the failure, and try the rest.
				 */
				exn = exn.addAlternative(alt, e);
			}
		}
		
		// All of the alternatives failed.
		throw exn;
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

	@Override
	public Set<Integer> freeVars() {
		return Type.allFreeVars(alts);
	}
}
