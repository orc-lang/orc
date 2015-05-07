//
// Datatype.java -- Java class Datatype
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

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

/**
 * A syntactic type encompassing all of the information associated with a datatype.
 * 
 * @author dkitchin
 */
public class Datatype extends Type {

	public TypeVariable typename;
	public List<List<Type>> members;
	public List<TypeVariable> formals;

	public Datatype(final TypeVariable typename, final List<List<Type>> members, final List<TypeVariable> formals) {
		this.typename = typename;
		this.members = members;
		this.formals = formals;
	}

	@Override
	public orc.ast.oil.type.Type convert(final Env<TypeVariable> env) throws TypeException {

		// First, add the datatype name itself to the context
		Env<TypeVariable> newenv = env.extend(typename);

		// Then, add the type parameters
		newenv = newenv.extendAll(formals);

		final List<List<orc.ast.oil.type.Type>> cs = new LinkedList<List<orc.ast.oil.type.Type>>();
		for (final List<Type> args : members) {
			cs.add(Type.convertAll(args, newenv));
		}

		return new orc.ast.oil.type.Datatype(cs, formals.size(), typename.name);
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(final Type T, final FreeTypeVariable X) {

		final List<List<Type>> cs = new LinkedList<List<Type>>();
		for (final List<Type> args : members) {
			cs.add(Type.substAll(args, T, X));
		}

		return new Datatype(typename, cs, formals);
	}

}
