//
// Tycon.java -- Java class Tycon
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

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypeInstance;

/**
 * Root class for all type constructors.
 * 
 * By default, the instance method for a tycon simply creates a TypeInstance
 * type of that tycon on the instance parameters. The TypeInstance class handles
 * subtype, meet, and join for such instances.
 * 
 * It is assumed that all children of Tycon will implement variances() such that
 * its size() > 0, but there is no way to enforce this condition in Java.
 * 
 * @author dkitchin
 */
public abstract class Tycon extends Type {

	/* Create an instance of a polymorphic type at the given parameters.
	 * 
	 * Currently, this is done by checking the type arity to make sure
	 * the number of parameters is correct, and then creating a type
	 * instance of that type with the parameters.
	 * 
	 */
	public Type instance(final List<Type> params) throws TypeException {

		if (variances().size() != params.size()) {
			throw new TypeArityException(variances().size(), params.size());
		}

		return new TypeInstance(this, params);
	}

	public Type instance() throws TypeException {
		return instance(new LinkedList<Type>());
	}

	/* Convenience function for the common case: a single type parameter. */
	public Type instance(final Type param) throws TypeException {
		final List<Type> ts = new LinkedList<Type>();
		ts.add(param);
		return instance(ts);
	}

	/* Make a callable instance of this tycon */
	/* By default, tycons do not have callable instances */
	public Type makeCallableInstance(final List<Type> params) throws TypeException {
		throw new TypeException("Cannot create a callable instance of type " + this);
	}

	/* By default, tycon equality is class equality */
	@Override
	public boolean equals(final Object that) {
		return that.getClass().equals(that.getClass());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public Tycon asTycon() {
		return this;
	}
}
