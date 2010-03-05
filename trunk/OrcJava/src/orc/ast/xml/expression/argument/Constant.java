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

package orc.ast.xml.expression.argument;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Constant extends Argument {
	@XmlElement(required = true, nillable = true)
	public Object value;

	public Constant() {
	}

	public Constant(final Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + value.getClass().toString() + "(" + value + "))";
	}

	@Override
	public orc.ast.oil.expression.argument.Argument unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.argument.Constant(value);
	}
}
