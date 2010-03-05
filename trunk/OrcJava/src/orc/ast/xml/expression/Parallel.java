//
// Parallel.java -- Java class Parallel
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

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Parallel extends Expression {
	@XmlElement(required = true)
	public Expression left;
	@XmlElement(required = true)
	public Expression right;

	public Parallel() {
	}

	public Parallel(final Expression left, final Expression right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + left + ", " + right + ")";
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.Parallel(left.unmarshal(config), right.unmarshal(config));
	}
}
