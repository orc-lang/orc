//
// Variance.java -- Java class Variance
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

package orc.type.tycon;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

/**
 * Variances of type parameters.
 * 
 * @author dkitchin
 */
public abstract class Variance {

	/* Find the conjunction of these two variances */
	public abstract Variance and(Variance that);

	/* Apply this variance to another variance,
	 * i.e. if a variable with that variance occurs in
	 * a context with this variance, what is the variable's
	 * variance in the larger context?
	 */
	public abstract Variance apply(Variance that);

	/* Invert this variance */
	public abstract Variance invert();

	/* Test whether S is a subtype of T under this variance */
	public abstract boolean subtype(Type S, Type T) throws TypeException;

	/* Find the join of S and T under this variance. */
	public abstract Type join(Type S, Type T) throws TypeException;

	/* Find the meet of S and T under this variance */
	public abstract Type meet(Type S, Type T) throws TypeException;

	/* Find the minimal substitution for X under this variance given S <: X <: T */
	public abstract Type minimum(Type S, Type T) throws TypeException;

	public static final Variance CONSTANT = new Constant();
	public static final Variance COVARIANT = new Covariant();
	public static final Variance CONTRAVARIANT = new Contravariant();
	public static final Variance INVARIANT = new Invariant();

	@Override
	public boolean equals(final Object that) {
		return this.getClass().equals(that.getClass());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

final class Constant extends Variance {

	@Override
	public Variance and(final Variance that) {
		return that;
	}

	@Override
	public Variance invert() {
		return CONSTANT;
	}

	@Override
	public Variance apply(final Variance that) {
		return CONSTANT;
	}

	@Override
	public boolean subtype(final Type S, final Type T) throws TypeException {
		return true;
	}

	@Override
	public Type join(final Type S, final Type T) throws TypeException {
		/* We could choose any type here, but for convenience we'll just choose S */
		return S;
	}

	@Override
	public Type meet(final Type S, final Type T) throws TypeException {
		/* We could choose any type here, but for convenience we'll just choose S */
		return S;
	}

	@Override
	public Type minimum(final Type S, final Type T) {
		return S;
	}
}

final class Covariant extends Variance {

	@Override
	public Variance and(final Variance that) {
		if (that instanceof Constant || that instanceof Covariant) {
			return this;
		} else {
			return INVARIANT;
		}
	}

	@Override
	public Variance invert() {
		return CONTRAVARIANT;
	}

	@Override
	public Variance apply(final Variance that) {
		return that;
	}

	@Override
	public boolean subtype(final Type S, final Type T) throws TypeException {
		return S.subtype(T);
	}

	@Override
	public Type join(final Type S, final Type T) throws TypeException {
		return S.join(T);
	}

	@Override
	public Type meet(final Type S, final Type T) throws TypeException {
		return S.meet(T);
	}

	@Override
	public Type minimum(final Type S, final Type T) {
		return S;
	}
}

final class Contravariant extends Variance {

	@Override
	public Variance and(final Variance that) {
		if (that instanceof Constant || that instanceof Contravariant) {
			return this;
		} else {
			return INVARIANT;
		}
	}

	@Override
	public Variance invert() {
		return COVARIANT;
	}

	@Override
	public Variance apply(final Variance that) {
		return that.invert();
	}

	@Override
	public boolean subtype(final Type S, final Type T) throws TypeException {
		return T.subtype(S);
	}

	@Override
	public Type join(final Type S, final Type T) throws TypeException {
		return S.meet(T);
	}

	@Override
	public Type meet(final Type S, final Type T) throws TypeException {
		return S.join(T);
	}

	@Override
	public Type minimum(final Type S, final Type T) {
		return T;
	}

}

final class Invariant extends Variance {
	@Override
	public Variance and(final Variance that) {
		return this;
	}

	@Override
	public Variance invert() {
		return INVARIANT;
	}

	@Override
	public Variance apply(final Variance that) {
		return INVARIANT;
	}

	@Override
	public boolean subtype(final Type S, final Type T) throws TypeException {
		return S.subtype(T) && T.subtype(S);
	}

	@Override
	public Type join(final Type S, final Type T) throws TypeException {
		if (S.equal(T)) {
			return S;
		} else {
			return Type.TOP;
		}
	}

	@Override
	public Type meet(final Type S, final Type T) throws TypeException {
		if (S.equal(T)) {
			return S;
		} else {
			return Type.BOT;
		}
	}

	@Override
	public Type minimum(final Type S, final Type T) throws TypeException {
		if (S.equal(T)) {
			return S;
		} else {
			throw new TypeException("Couldn't infer a minimal type; please add an explicit type argument, e.g. Buffer[Integer](). (Lower bound " + S + " and upper bound " + T + " are not equal, but both are candidates for an invariant argument position.)");
		}
	}
}
