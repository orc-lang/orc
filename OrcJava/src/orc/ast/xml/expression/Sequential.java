//
// Sequential.java -- Java class Sequential
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

package orc.ast.xml.expression;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Sequential extends Expression {
	@XmlElement(required = true)
	public Expression left;
	@XmlElement(required = true)
	public Expression right;
	@XmlAttribute(required = false)
	public String name;

	public Sequential() {
	}

	public Sequential(final Expression left, final Expression right, final String name) {
		this.left = left;
		this.right = right;
		this.name = name;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + left + ", " + right + ")";
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.Sequential(left.unmarshal(config), right.unmarshal(config), name);
	}
}
