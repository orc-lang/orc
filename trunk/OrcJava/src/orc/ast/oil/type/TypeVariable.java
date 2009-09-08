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

package orc.ast.oil.type;

import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;

/**
 * A bound type variable.
 * 
 * Type variables in OIL, like program variables, are
 * represented namelessly by deBruijn indices. 
 *
 * @author dkitchin
 */
public class TypeVariable extends Type {

	/* An optional string name to use for this variable in debugging contexts. */
	public int index;
	public String name = null;
	
	public TypeVariable(int index, String name) {
		this.name = name;
		this.index = index;
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#convert(orc.env.Env)
	 */
	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		return new orc.type.TypeVariable(index, name);
	}
	
	public String toString() {
		return (name != null ? name : super.toString());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.TypeVariable(index, name);
	}

}
