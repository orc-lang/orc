//
// ArrowType.java -- Java class ArrowType
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
import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.inference.Constraint;
import orc.type.tycon.Variance;

public class ArrowType extends Type {

	public List<Type> argTypes;
	public Type resultType;
	public int typeArity = 0;

	public ArrowType(final Type resultType) {
		this.argTypes = new LinkedList<Type>();
		this.resultType = resultType;
	}

	public ArrowType(final Type argType, final Type resultType) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(argType);
		this.resultType = resultType;
	}

	public ArrowType(final Type firstArgType, final Type secondArgType, final Type resultType) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(firstArgType);
		argTypes.add(secondArgType);
		this.resultType = resultType;
	}

	public ArrowType(final List<Type> argTypes, final Type resultType) {
		this.argTypes = argTypes;
		this.resultType = resultType;
	}

	public ArrowType(final Type resultType, final int typeArity) {
		this.argTypes = new LinkedList<Type>();
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public ArrowType(final Type argType, final Type resultType, final int typeArity) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(argType);
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public ArrowType(final Type firstArgType, final Type secondArgType, final Type resultType, final int typeArity) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(firstArgType);
		argTypes.add(secondArgType);
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public ArrowType(final List<Type> argTypes, final Type resultType, final int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	/*
	 * Checks that the given type is in fact an
	 * ArrowType of the same type and argument 
	 * arity as this arrow type.
	 * If so, returns that type, cast to ArrowType.
	 * Otherwise, returns null. 
	 * 
	 */
	protected ArrowType forceArrow(final Type that) {

		if (that instanceof ArrowType) {
			final ArrowType thatArrow = (ArrowType) that;
			if (argTypes.size() == thatArrow.argTypes.size() && typeArity == thatArrow.typeArity) {
				return thatArrow;
			}
		}

		return null;
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {
		final ArrowType thatArrow = forceArrow(that);
		if (thatArrow != null) {
			final List<Type> otherArgTypes = thatArrow.argTypes;

			/*
			 * Arguments are contravariant: make sure
			 * that each other arg type is a subtype
			 * of this arg type.
			 */
			for (int i = 0; i < argTypes.size(); i++) {
				final Type thisArg = argTypes.get(i);
				final Type otherArg = otherArgTypes.get(i);
				if (!otherArg.subtype(thisArg)) {
					return false;
				}
			}

			/*
			 * Result type is covariant.
			 */
			return this.resultType.subtype(thatArrow.resultType);
		} else {
			return super.subtype(that);
		}
	}

	/* 
	 * A join of two arrow types is a meet of their arg types
	 * and a join of their result type.
	 */
	@Override
	public Type join(final Type that) throws TypeException {

		final ArrowType thatArrow = forceArrow(that);
		if (thatArrow != null) {
			final List<Type> otherArgTypes = thatArrow.argTypes;

			final List<Type> joinArgTypes = new LinkedList<Type>();
			Type joinResultType;

			for (int i = 0; i < argTypes.size(); i++) {
				final Type thisArg = argTypes.get(i);
				final Type otherArg = otherArgTypes.get(i);
				joinArgTypes.add(thisArg.meet(otherArg));
			}

			joinResultType = this.resultType.join(thatArrow.resultType);

			return new ArrowType(joinArgTypes, joinResultType);
		} else {
			return super.join(that);
		}
	}

	/* 
	 * A meet of two arrow types is a join of their arg types
	 * and a meet of their result type.
	 */
	@Override
	public Type meet(final Type that) throws TypeException {

		final ArrowType thatArrow = forceArrow(that);
		if (thatArrow != null) {

			final List<Type> otherArgTypes = thatArrow.argTypes;

			final List<Type> meetArgTypes = new LinkedList<Type>();
			Type meetResultType;

			for (int i = 0; i < argTypes.size(); i++) {
				final Type thisArg = argTypes.get(i);
				final Type otherArg = otherArgTypes.get(i);
				meetArgTypes.add(thisArg.join(otherArg));
			}

			meetResultType = this.resultType.meet(thatArrow.resultType);

			return new ArrowType(meetArgTypes, meetResultType);
		} else {
			return super.meet(that);
		}

	}

	@Override
	public Type call(TypingContext ctx, final List<Argument> args, List<Type> typeActuals) throws TypeException {

		/* Arity check */
		if (argTypes.size() != args.size()) {
			throw new ArgumentArityException(argTypes.size(), args.size());
		}

		/* Inference request check */
		if (typeActuals == null) {
			if (typeArity > 0) {
				return ctx.requestInference(this);
			} else {
				/* Just use an empty list */
				typeActuals = new LinkedList<Type>();
			}
		}
		/* Type arity check */
		else {
			if (typeArity != typeActuals.size()) {
				throw new TypeArityException(typeArity, typeActuals.size());
			}
		}

		/* Add each type argument to the type context */
		for (final Type targ : typeActuals) {
			ctx = ctx.bindType(targ);
		}

		/* Check each argument against its respective argument type */
		for (int i = 0; i < argTypes.size(); i++) {
			final Type thisType = ctx.subst(argTypes.get(i));
			final Argument thisArg = args.get(i);
			thisArg.typecheck(ctx, thisType);
		}

		return ctx.subst(resultType);
	}

	@Override
	public Type subst(final Env<Type> ctx) throws TypeException {

		Env<Type> newctx = ctx;

		/* Add empty entries in the context for each bound type parameter */
		for (int i = 0; i < typeArity; i++) {
			newctx = newctx.extend(null);
		}

		return new ArrowType(Type.substAll(argTypes, newctx), resultType.subst(newctx), typeArity);
	}

	@Override
	public Variance findVariance(final Integer var) {

		Variance result = resultType.findVariance(var);

		for (final Type T : argTypes) {
			final Variance v = T.findVariance(var).invert();
			result = result.and(v);
		}

		return result;
	}

	@Override
	public Type promote(Env<Boolean> V) throws TypeException {

		// Exclude newly bound variables from the set V
		for (int i = 0; i < typeArity; i++) {
			V = V.extend(false);
		}

		final Type newResultType = resultType.promote(V);

		final List<Type> newArgTypes = new LinkedList<Type>();
		for (final Type T : argTypes) {
			newArgTypes.add(T.demote(V));
		}

		return new ArrowType(newArgTypes, newResultType, typeArity);
	}

	@Override
	public Type demote(Env<Boolean> V) throws TypeException {

		// Exclude newly bound variables from the set V
		for (int i = 0; i < typeArity; i++) {
			V = V.extend(false);
		}

		final Type newResultType = resultType.demote(V);

		final List<Type> newArgTypes = new LinkedList<Type>();
		for (final Type T : argTypes) {
			newArgTypes.add(T.promote(V));
		}

		return new ArrowType(newArgTypes, newResultType, typeArity);
	}

	@Override
	public void addConstraints(Env<Boolean> VX, final Type T, final Constraint[] C) throws TypeException {

		if (T instanceof ArrowType) {
			final ArrowType other = (ArrowType) T;

			if (other.argTypes.size() != argTypes.size() || other.typeArity != typeArity) {
				throw new SubtypeFailureException(this, T);
			}

			for (int i = 0; i < typeArity; i++) {
				VX = VX.extend(true);
			}

			for (int i = 0; i < argTypes.size(); i++) {
				final Type A = argTypes.get(i);
				final Type B = other.argTypes.get(i);

				B.addConstraints(VX, A, C);
			}
			resultType.addConstraints(VX, other.resultType, C);

		} else {
			super.addConstraints(VX, T, C);
		}
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');

		s.append("lambda ");
		if (typeArity > 0) {
			s.append("[_");
			for (int i = 1; i < typeArity; i++) {
				s.append(",_");
			}
			s.append("]");
		}
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

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		final orc.ast.xml.type.Type[] newArgTypes = new orc.ast.xml.type.Type[argTypes.size()];
		int i = 0;
		for (final Type t : argTypes) {
			newArgTypes[i] = t.marshal();
			++i;
		}
		orc.ast.xml.type.Type newResultType = null;
		if (resultType != null) {
			newResultType = resultType.marshal();
		}
		return new orc.ast.xml.type.ArrowType(newArgTypes, newResultType, typeArity);
	}

	@Override
	public Set<Integer> freeVars() {

		final Set<Integer> vars = Type.allFreeVars(argTypes);
		vars.addAll(resultType.freeVars());

		return Type.shiftFreeVars(vars, typeArity);
	}
}
