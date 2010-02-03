//
// NamedType.java -- Java class NamedType
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

package orc.ast.extended.type;

/**
 * A simple named type.
 * 
 * @author dkitchin
 *
 */
public class NamedType extends Type {

	public String name;

	public NamedType(final String name) {
		this.name = name;
	}

	@Override
	public orc.ast.simple.type.Type simplify() {
		return new orc.ast.simple.type.FreeTypeVariable(name);
	}

	@Override
	public String toString() {
		return name;
	}

}
