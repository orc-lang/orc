//
// EllipsisArrowType.java -- Java class EllipsisArrowType
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

import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.ground.Top;

public class EllipsisArrowType extends Type {

	public Type repeatedArgType;
	public Type resultType;

	public EllipsisArrowType(final Type repeatedArgType, final Type resultType) {
		this.repeatedArgType = repeatedArgType;
		this.resultType = resultType;
	}

	protected ArrowType makeArrow(final int arity) {

		final List<Type> argTypes = new LinkedList<Type>();

		for (int i = 0; i < arity; i++) {
			argTypes.add(repeatedArgType);
		}

		return new ArrowType(argTypes, resultType);
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {

		if (that instanceof Top) {
			return true;
		}

		if (that instanceof EllipsisArrowType) {
			final EllipsisArrowType thatEA = (EllipsisArrowType) that;

			return thatEA.repeatedArgType.subtype(this.repeatedArgType) && this.resultType.subtype(thatEA.resultType);
		} else if (that instanceof ArrowType) {
			final ArrowType thatArrow = (ArrowType) that;
			final ArrowType thisArrow = makeArrow(thatArrow.argTypes.size());

			return thisArrow.subtype(thatArrow);
		} else {
			return false;
		}
	}

	@Override
	public Type join(final Type that) throws TypeException {

		if (that instanceof EllipsisArrowType) {
			final EllipsisArrowType thatEA = (EllipsisArrowType) that;

			final Type joinRAT = repeatedArgType.meet(thatEA.repeatedArgType);
			final Type joinRT = resultType.join(thatEA.resultType);
			return new EllipsisArrowType(joinRAT, joinRT);
		} else if (that instanceof ArrowType) {
			final ArrowType thatArrow = (ArrowType) that;
			final ArrowType thisArrow = makeArrow(thatArrow.argTypes.size());

			return thisArrow.join(thatArrow);
		} else {
			return Type.TOP;
		}
	}

	@Override
	public Type meet(final Type that) throws TypeException {

		if (that instanceof EllipsisArrowType) {
			final EllipsisArrowType thatEA = (EllipsisArrowType) that;

			final Type joinRAT = repeatedArgType.join(thatEA.repeatedArgType);
			final Type joinRT = resultType.meet(thatEA.resultType);
			return new EllipsisArrowType(joinRAT, joinRT);
		} else if (that instanceof ArrowType) {
			final ArrowType thatArrow = (ArrowType) that;
			final ArrowType thisArrow = makeArrow(thatArrow.argTypes.size());

			return thisArrow.meet(thatArrow);
		} else {
			return Type.TOP;
		}
	}

	@Override
	public Type call(final List<Type> args) throws TypeException {

		for (final Type T : args) {
			if (!T.subtype(repeatedArgType)) {
				throw new SubtypeFailureException(T, repeatedArgType);
			}
		}

		return resultType;
	}

	@Override
	public Set<Integer> freeVars() {

		final Set<Integer> vars = repeatedArgType.freeVars();
		vars.addAll(resultType.freeVars());

		return vars;
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');
		s.append(repeatedArgType);
		s.append("... -> ");
		s.append(resultType);
		s.append(')');

		return s.toString();
	}

}
