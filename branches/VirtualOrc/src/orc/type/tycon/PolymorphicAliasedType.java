//
// PolymorphicAliasedType.java -- Java class PolymorphicAliasedType
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

package orc.type.tycon;

import java.util.List;
import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.Type;

/**
 * 
 * A type-level representation of a user-defined type alias with
 * type parameters. Essentially this is a user-defined type constructor;
 * the variances of the type parameters are inferred automatically
 * from the aliased type itself.
 * 
 * @author dkitchin
 *
 */
public class PolymorphicAliasedType extends Tycon {

	public List<Variance> inferredVariances;
	public Type type;

	public PolymorphicAliasedType(final Type type, final List<Variance> inferredVariances) {
		this.inferredVariances = inferredVariances;
		this.type = type;
	}

	/* Parametric type aliases instantiate to the aliased type,
	 * with the appropriate substitutions.
	 * 
	 * (non-Javadoc)
	 * @see orc.type.Tycon#instance(java.util.List)
	 */
	@Override
	public Type instance(final List<Type> params) throws TypeException {

		Env<Type> subctx = new Env<Type>();
		for (final Type t : params) {
			subctx = subctx.extend(t);
		}

		return type.subst(subctx);
	}

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		return new orc.ast.xml.type.PolymorphicTypeAlias(type.marshal(), inferredVariances.size());
	}

	@Override
	public Set<Integer> freeVars() {
		return Type.shiftFreeVars(type.freeVars(), inferredVariances.size());
	}

}
