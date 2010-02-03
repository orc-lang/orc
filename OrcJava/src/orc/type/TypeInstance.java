//
// TypeInstance.java -- Java class TypeInstance
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

package orc.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.argument.Argument;
import orc.env.Env;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.java.ClassTycon;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

/**
 * A type constructor instantiated at particular types, 
 * e.g. a List of Integers.
 * 
 * The type params may be empty in the special case that a type instance
 * is constructed and substituted by the compiler itself, for example
 * when binding a declared Java class with no generic parameters 
 * in Orc type space. At present, users should not be able to write types
 * using type applications to an empty parameter list.
 * 
 * @author dkitchin
 */
public class TypeInstance extends Type {

	public Tycon tycon;
	public List<Type> params;

	public TypeInstance(final Tycon tycon, final List<Type> params) {
		this.tycon = tycon;
		this.params = params;
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {

		/* The other type must also be an instance */
		if (that instanceof TypeInstance) {
			final TypeInstance thatInstance = (TypeInstance) that;

			/* If this type instance actually has no parameters, then it suffices for
			 * the tycons to be in a subtype relationship.
			 */
			if (tycon.variances().size() == 0) {
				return tycon.subtype(thatInstance.tycon);
			}

			/* Otherwise, the tycon of that instance must be equal to this tycon */
			if (tycon.equals(thatInstance.tycon)) {

				final List<Type> otherParams = thatInstance.params;
				final List<Variance> vs = tycon.variances();

				for (int i = 0; i < vs.size(); i++) {
					final Variance v = vs.get(i);
					/* Make sure none of the type parameters fail to obey their variance restrictions */
					if (!v.subtype(params.get(i), otherParams.get(i))) {
						return false;
					}
				}
				/* If the other type is another instance of the same type, whose
				 * parameters vary appropriately, then this is a subtype of that type.
				 */
				return true;
			}
		}

		/* Otherwise, the only other supertype of a type instance is Top */
		return that instanceof Top;
	}

	@Override
	public Type join(final Type that) throws TypeException {

		/* The other type must also be an instance */
		if (that instanceof TypeInstance) {
			final TypeInstance thatInstance = (TypeInstance) that;

			/* The tycon of that instance must be equal to this tycon */
			if (tycon.equals(thatInstance.tycon)) {

				final List<Type> otherParams = thatInstance.params;
				final List<Variance> vs = tycon.variances();
				final List<Type> joinParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					final Variance v = vs.get(i);
					final Type joinType = v.join(params.get(i), otherParams.get(i));
					joinParams.add(joinType);
				}

				return new TypeInstance(tycon, joinParams);
			}
		}

		/* If we cannot find a join, delegate to super */
		return super.join(that);
	}

	@Override
	public Type meet(final Type that) throws TypeException {

		/* The other type must also be an application */
		if (that instanceof TypeInstance) {
			final TypeInstance thatInstance = (TypeInstance) that;

			/* The head type of that application must be equal to this type */
			if (tycon.equals(thatInstance.tycon)) {

				final List<Type> otherParams = thatInstance.params;
				final List<Variance> vs = tycon.variances();
				final List<Type> meetParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					final Variance v = vs.get(i);
					meetParams.add(v.meet(params.get(i), otherParams.get(i)));
				}

				return new TypeInstance(tycon, meetParams);
			}
		}

		/* If we cannot find a meet, delegate to super */
		return super.meet(that);
	}

	/* Call the type as a type instance using the params */
	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {
		return tycon.makeCallableInstance(params).call(ctx, args, typeActuals);
	}

	@Override
	public Type call(final List<Type> args) throws TypeException {
		return tycon.makeCallableInstance(params).call(args);
	}

	@Override
	public Type subst(final Env<Type> ctx) throws TypeException {
		return new TypeInstance(tycon, Type.substAll(params, ctx));
	}

	@Override
	public Variance findVariance(final Integer var) {

		Variance result = Variance.CONSTANT;

		final List<Variance> vs = tycon.variances();
		for (int i = 0; i < vs.size(); i++) {
			final Variance v = vs.get(i);
			final Type p = params.get(i);
			result = result.and(v.apply(p.findVariance(var)));
		}

		return result;
	}

	@Override
	public Type promote(final Env<Boolean> V) throws TypeException {

		final List<Type> newParams = new LinkedList<Type>();

		final List<Variance> vs = tycon.variances();
		for (int i = 0; i < vs.size(); i++) {
			final Variance v = vs.get(i);
			final Type p = params.get(i);

			Type newp;
			if (v.equals(Variance.INVARIANT)) {
				if (p.equals(p.promote(V))) {
					newp = p;
				} else {
					// TODO: Make this less cryptic
					throw new TypeException("Could not infer type parameters; an invariant position is overconstrained");
				}
			} else if (v.equals(Variance.CONTRAVARIANT)) {
				newp = p.demote(V);
			} else {
				newp = p.promote(V);
			}

			newParams.add(newp);
		}

		return new TypeInstance(tycon, newParams);
	}

	@Override
	public Type demote(final Env<Boolean> V) throws TypeException {

		final List<Type> newParams = new LinkedList<Type>();

		final List<Variance> vs = tycon.variances();
		for (int i = 0; i < vs.size(); i++) {
			final Variance v = vs.get(i);
			final Type p = params.get(i);

			Type newp;
			if (v.equals(Variance.INVARIANT)) {
				if (p.equals(p.demote(V))) {
					newp = p;
				} else {
					// TODO: Make this less cryptic
					throw new TypeException("Could not infer type parameters; an invariant position is overconstrained");
				}
			} else if (v.equals(Variance.CONTRAVARIANT)) {
				newp = p.promote(V);
			} else {
				newp = p.demote(V);
			}

			newParams.add(newp);
		}

		return new TypeInstance(tycon, newParams);
	}

	@Override
	public void addConstraints(final Env<Boolean> VX, final Type T, final Constraint[] C) throws TypeException {

		if (T instanceof TypeInstance) {
			final TypeInstance otherApp = (TypeInstance) T;

			if (!otherApp.tycon.equals(tycon) || otherApp.params.size() != params.size()) {
				throw new SubtypeFailureException(this, T);
			}

			final List<Variance> vs = tycon.variances();
			for (int i = 0; i < vs.size(); i++) {
				final Variance v = vs.get(i);
				final Type A = params.get(i);
				final Type B = otherApp.params.get(i);

				if (v.equals(Variance.COVARIANT)) {
					A.addConstraints(VX, B, C);
				} else if (v.equals(Variance.CONTRAVARIANT)) {
					B.addConstraints(VX, A, C);
				} else if (v.equals(Variance.INVARIANT)) {
					A.addConstraints(VX, B, C);
					B.addConstraints(VX, A, C);
				}
			}

		} else {
			super.addConstraints(VX, T, C);
		}
	}

	/* Make sure that this type is an application of the given type 
	 * (or some subtype) to exactly one type parameter. If so, return the parameter, and
	 * if not raise an error.
	 */
	@Override
	public Type unwrapAs(final Type T) throws TypeException {

		if (tycon.subtype(T)) {
			if (params.size() == 1) {
				return params.get(0);
			} else {
				throw new TypeArityException(1, params.size());
			}
		} else {
			throw new SubtypeFailureException(T, tycon);
		}

	}

	@Override
	public Class javaCounterpart() {

		if (tycon instanceof ClassTycon) {
			final ClassTycon ct = (ClassTycon) tycon;
			if (ct.cls.getTypeParameters().length == 0) {
				return ct.cls;
			}
		}

		return null;
	}

	@Override
	public Set<Integer> freeVars() {

		final Set<Integer> vars = Type.allFreeVars(params);
		vars.addAll(tycon.freeVars());

		return vars;
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append(tycon.toString());
		if (params.size() > 0) {
			s.append('[');
			for (int i = 0; i < params.size(); i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(params.get(i));
			}
			s.append(']');
		}

		return s.toString();
	}

}
