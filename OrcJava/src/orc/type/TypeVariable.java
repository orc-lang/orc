//
// TypeVariable.java -- Java class TypeVariable
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

import java.util.Set;
import java.util.TreeSet;

import orc.env.Env;
import orc.env.LookupFailureException;
import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.tycon.Variance;

/**
 * A bound type variable.
 * 
 * Subtype comparisons may occur between types with bound variables (such as between
 * the type bodies of parameterized arrow types), so there is a subtype relation
 * specified for type variables: it is simply variable equality.
 * 
 * @author dkitchin
 */
public class TypeVariable extends Type {

	public int index;
	public String name = null; // Optional program text name, used only for display purposes

	public TypeVariable(final int index) {
		this.index = index;
	}

	public TypeVariable(final int index, final String name) {
		this.index = index;
		this.name = name;
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {

		if (that instanceof TypeVariable) {
			return this.index == ((TypeVariable) that).index;
		} else {
			return that instanceof Top;
		}
	}

	@Override
	public Type subst(final Env<Type> ctx) {

		Type t;

		try {
			t = ctx.lookup(index);
		} catch (final LookupFailureException e) {
			throw new OrcError(e);
		}

		/* If t is null, then this is a bound variable and should not be replaced */
		return t != null ? t : this;
	}

	@Override
	public Variance findVariance(final Integer var) {
		return var == index ? Variance.COVARIANT : Variance.CONSTANT;
	}

	@Override
	public Type promote(final Env<Boolean> V) {
		try {
			return V.lookup(index) ? Type.TOP : this;
		} catch (final LookupFailureException e) {
			return this;
		}
	}

	@Override
	public Type demote(final Env<Boolean> V) {
		try {
			return V.lookup(index) ? Type.BOT : this;
		} catch (final LookupFailureException e) {
			return this;
		}
	}

	@Override
	public void addConstraints(final Env<Boolean> VX, final Type T, final Constraint[] C) throws TypeException {

		try {
			if (!VX.lookup(index)) {
				/* this is in X */

				// Find Z, the index of this variable in the outer context
				int Z;
				try {
					Z = index - VX.search(false);
				} catch (final SearchFailureException e) {
					throw new OrcError(e);
				}

				/* Demote the type to remove the variables in V,
				 * and then add it as an upper bound of Z.
				 */
				C[Z].atMost(T.demote(VX));
				return;
			} else {
				/* this is in V */
				super.addConstraints(VX, T, C);
			}
		}
		/* It is also possible that this variable is not in V or X;
		 * it is a bound type variable from an enclosing scope.
		 * In this case, just treat it as an opaque type.
		 */
		catch (final LookupFailureException e) {
			/* this is bound outside of V or X */
			/* This occurs when checking under a type binder */
			// TODO: Add bounded polymorphism support.
			super.addConstraints(VX, T, C);
		}
	}

	@Override
	public String toString() {
		return name == null ? "#" + index : name;
	}

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		return new orc.ast.xml.type.TypeVariable(index);
	}

	@Override
	public Set<Integer> freeVars() {
		final Set<Integer> singleton = new TreeSet<Integer>();
		singleton.add(index);
		return singleton;
	}
}
