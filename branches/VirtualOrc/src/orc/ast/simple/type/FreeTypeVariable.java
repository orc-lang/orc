//
// FreeTypeVariable.java -- Java class FreeTypeVariable
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
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnboundTypeException;

/**
 *
 * A free type variable.
 *
 * @author dkitchin
 */
public class FreeTypeVariable extends Type implements Comparable<FreeTypeVariable> {

	public String name;

	public FreeTypeVariable(final String name) {
		this.name = name;
	}

	public int compareTo(final FreeTypeVariable f) {
		final String s = this.name;
		final String t = f.name;
		return s.compareTo(t);
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#convert(orc.env.Env)
	 */
	@Override
	public orc.ast.oil.type.Type convert(final Env<TypeVariable> env) throws TypeException {
		throw new UnboundTypeException(name);
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(final Type T, final FreeTypeVariable X) {
		return this.name.equals(X.name) ? T : this;
	}

	@Override
	public String toString() {
		return name;
	}

}
