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

package orc.type.structured;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.Type;
import orc.type.ground.ConstIntType;
import orc.type.ground.IntegerType;
import orc.type.ground.Message;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.tycon.Variance;

public class TupleType extends Type {

	public List<Type> items;

	public TupleType(final List<Type> items) {
		this.items = items;
	}

	/* Convenience function for constructing pairs */
	public TupleType(final Type a, final Type b) {
		this.items = new LinkedList<Type>();
		this.items.add(a);
		this.items.add(b);
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {

		if (that instanceof Top) {
			return true;
		}

		if (that instanceof TupleType) {

			final TupleType other = (TupleType) that;

			if (width() != other.width()) {
				return false;
			}

			final List<Type> otherItems = other.items;
			for (int i = 0; i < width(); i++) {
				final Type thisItem = items.get(i);
				final Type otherItem = otherItems.get(i);

				if (!thisItem.subtype(otherItem)) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public Type join(final Type that) throws TypeException {

		if (that instanceof TupleType) {

			final TupleType other = (TupleType) that;

			if (width() != other.width()) {
				return Type.TOP;
			}

			final List<Type> otherItems = other.items;
			final List<Type> joinItems = new LinkedList<Type>();
			for (int i = 0; i < width(); i++) {
				final Type thisItem = items.get(i);
				final Type otherItem = otherItems.get(i);

				joinItems.add(thisItem.join(otherItem));
			}

			return new TupleType(joinItems);
		} else {
			return super.join(that);
		}

	}

	@Override
	public Type meet(final Type that) throws TypeException {

		if (that instanceof TupleType) {

			final TupleType other = (TupleType) that;

			if (width() != other.width()) {
				return Type.BOT;
			}

			final List<Type> otherItems = other.items;
			final List<Type> meetItems = new LinkedList<Type>();
			for (int i = 0; i < width(); i++) {
				final Type thisItem = items.get(i);
				final Type otherItem = otherItems.get(i);

				meetItems.add(thisItem.meet(otherItem));
			}

			return new TupleType(meetItems);
		} else {
			return super.meet(that);
		}
	}

	@Override
	public Type call(final List<Type> args) throws TypeException {

		// TODO: Need a more general solution for messages; this is not quite correct.
		if (args.size() == 1) {
			final Type T = args.get(0);
			if (T instanceof Message) {
				final Message m = (Message) T;
				// TODO: Make a set of special compiler constants
				if (m.f.key.equals("fits")) {
					return new ArrowType(Type.INTEGER, Type.BOOLEAN);
				}
			}
		}

		if (args.size() == 1) {

			final Type T = args.get(0);
			if (T instanceof ConstIntType) {
				final Integer index = ((ConstIntType) T).i;
				if (index < width()) {
					return items.get(index);
				} else {
					// TODO: Make this a more specific exception type
					throw new TypeException("Can't access index " + index + " of a " + width() + " element tuple (indices start at 0)");
				}
			} else if (T instanceof IntegerType) {
				Type j = Type.BOT;
				for (final Type iType : items) {
					j = j.join(iType);
				}
				return j;
			} else {
				throw new SubtypeFailureException(T, Type.INTEGER);
			}
		} else {
			throw new ArgumentArityException(1, args.size());
		}

	}

	@Override
	public Type subst(final Env<Type> ctx) throws TypeException {
		return new TupleType(Type.substAll(items, ctx));
	}

	@Override
	public Variance findVariance(final Integer var) {

		Variance result = Variance.CONSTANT;

		for (final Type T : items) {
			result = result.and(T.findVariance(var));
		}

		return result;
	}

	@Override
	public Type promote(final Env<Boolean> V) throws TypeException {

		final List<Type> newItems = new LinkedList<Type>();
		for (final Type T : items) {
			newItems.add(T.promote(V));
		}

		return new TupleType(newItems);
	}

	@Override
	public Type demote(final Env<Boolean> V) throws TypeException {

		final List<Type> newItems = new LinkedList<Type>();
		for (final Type T : items) {
			newItems.add(T.demote(V));
		}

		return new TupleType(newItems);
	}

	@Override
	public void addConstraints(final Env<Boolean> VX, final Type T, final Constraint[] C) throws TypeException {

		if (T instanceof TupleType) {
			final TupleType other = (TupleType) T;

			if (other.items.size() != items.size()) {
				throw new SubtypeFailureException(this, T);
			}

			for (int i = 0; i < items.size(); i++) {
				final Type A = items.get(i);
				final Type B = other.items.get(i);

				A.addConstraints(VX, B, C);
			}

		} else {
			super.addConstraints(VX, T, C);
		}
	}

	@Override
	public Set<Integer> freeVars() {
		return Type.allFreeVars(items);
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');
		for (int i = 0; i < width(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(items.get(i));
		}
		s.append(')');

		return s.toString();
	}

	public int width() {
		return items.size();
	}

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		final orc.ast.xml.type.Type[] newItems = new orc.ast.xml.type.Type[items.size()];
		int i = 0;
		for (final Type t : items) {
			newItems[i] = t.marshal();
			++i;
		}
		return new orc.ast.xml.type.TupleType(newItems);
	}
}
