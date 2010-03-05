//
// Top.java -- Java class Top
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

package orc.type.ground;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

/**
 * The Top type. Supertype of all other types.
 * 
 * All other types extend this type, so that we can use the Java
 * inheritance hierarchy to maintain a default subtyping relation.
 * 
 * The Top type can be ascribed to all values, and thus
 * necessarily carries no information.
 * 
 * @author dkitchin
 */
public final class Top extends Type {

	@Override
	public boolean subtype(final Type that) throws TypeException {
		return that.isTop();
	}

	@Override
	public boolean isTop() {
		return true;
	}

	@Override
	public String toString() {
		return "Top";
	}

	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.Top();
	}
}
