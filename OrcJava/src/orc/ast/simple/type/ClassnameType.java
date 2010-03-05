//
// ClassnameType.java -- Java class ClassnameType
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

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

/**
 * A type which refers to a Java class (which we will treat as an Orc type).
 * 
 * @author quark, dkitchin
 */
public class ClassnameType extends Type {

	public String classname;

	public ClassnameType(final String classname) {
		this.classname = classname;
	}

	@Override
	public orc.ast.oil.type.Type convert(final Env<orc.ast.simple.type.TypeVariable> env) throws TypeException {
		return new orc.ast.oil.type.ClassType(classname);
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
		return classname;
	}

}
