//
// WithLocation.java -- Java class WithLocation
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
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public class WithLocation extends Expression {
	@XmlElement(required = true)
	public Expression body;
	@XmlElement(required = true)
	public SourceLocation location;

	public WithLocation() {
	}

	public WithLocation(final Expression body, final SourceLocation location) {
		this.body = body;
		this.location = location;
	}

	@Override
	public String toString() {
		return body.toString();
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.WithLocation(body.unmarshal(config), location);
	}
}
