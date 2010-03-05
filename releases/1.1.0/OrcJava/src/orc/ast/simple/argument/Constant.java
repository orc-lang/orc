//
// Constant.java -- Java class Constant
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

package orc.ast.simple.argument;

import orc.env.Env;

/**
 * Program constants, which occur in argument position. 
 * 
 * @author dkitchin
 */
public class Constant extends Argument {
	public Object v;

	public Constant(final Object v) {
		this.v = v;
	}

	@Override
	public String toString() {
		return String.valueOf(v);
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(final Env<Variable> vars) {
		return new orc.ast.oil.expression.argument.Constant(v);
	}
}
