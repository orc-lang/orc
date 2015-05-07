//
// TypeVariable.java -- Java class TypeVariable
// Project OrcJava
//
// $Id$
//
// Created by dkitchin on Aug 26, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.simple.type;

import orc.env.Env;
import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.TypeException;

/**
 * 
 * A bound type variable.
 *
 * @author dkitchin
 */
public class TypeVariable extends Type {

	/* An optional string name to use for this variable in debugging contexts. */
	public String name = null;

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#convert(orc.env.Env)
	 */
	@Override
	public orc.ast.oil.type.Type convert(final Env<TypeVariable> env) throws TypeException {

		try {
			return new orc.ast.oil.type.TypeVariable(env.search(this), name);
		} catch (final SearchFailureException e) {
			e.printStackTrace();
			throw new OrcError("Could not find bound variable " + name + " in environment");
		}

	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(final Type T, final FreeTypeVariable X) {
		return this;
	}

	@Override
	public String toString() {
		return name != null ? name : super.toString();
	}

}
