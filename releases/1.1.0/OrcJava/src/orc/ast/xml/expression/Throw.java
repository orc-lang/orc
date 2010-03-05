//
// Throw.java -- Java class Throw
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

/**
 * The "throw" keyword.
 *
 * @author kid.andrew
 */
public class Throw extends Expression {
	@XmlElement(required = true)
	public Expression body;

	public Throw() {
	}

	public Throw(final Expression body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "(throw " + body.toString() + ")";
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.Throw(body.unmarshal(config));
	}
}
