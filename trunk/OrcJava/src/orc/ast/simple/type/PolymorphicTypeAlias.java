//
// PolymorphicTypeAlias.java -- Java class PolymorphicTypeAlias
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

package orc.ast.simple.type;

import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

/**
 * A representation of an aliased type with type parameters.
 * 
 * @author dkitchin
 */
public class PolymorphicTypeAlias extends Type {

	public Type type;
	public List<TypeVariable> formals;

	public PolymorphicTypeAlias(final Type type, final List<TypeVariable> formals) {
		this.type = type;
		this.formals = formals;
	}

	@Override
	public orc.ast.oil.type.Type convert(final Env<TypeVariable> env) throws TypeException {
		return new orc.ast.oil.type.PolymorphicTypeAlias(type.convert(env.extendAll(formals)), formals.size());
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(final Type T, final FreeTypeVariable X) {
		return this;
	}

}
