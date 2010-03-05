//
// DeclareDefs.java -- Java class DeclareDefs
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

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class DeclareDefs extends Expression {
	@XmlElement(name = "definition", required = true)
	public Def[] definitions;
	@XmlElement(required = true)
	public Expression body;

	public DeclareDefs() {
	}

	public DeclareDefs(final Def[] definitions, final Expression body) {
		this.definitions = definitions;
		this.body = body;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + Arrays.toString(definitions) + ", " + body + ")";
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		final LinkedList<orc.ast.oil.expression.Def> defs = new LinkedList<orc.ast.oil.expression.Def>();
		for (final Def d : definitions) {
			defs.add(d.unmarshal(config));
		}
		return new orc.ast.oil.expression.DeclareDefs(defs, body.unmarshal(config));
	}
}
