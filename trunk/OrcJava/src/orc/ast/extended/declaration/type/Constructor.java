//
// Constructor.java -- Java class Constructor
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

package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.expression.Expression;
import orc.ast.extended.type.Type;

/**
 * A single constructor in a variant type.
 * 
 * @author dkitchin
 */
public class Constructor {

	public String name;
	public List<Type> args;

	public Constructor(final String name, final List<Type> args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString() {
		return name + "(" + Expression.join(args, ",") + ")";
	}

}
