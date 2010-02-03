//
// DeclareType.java -- Java class DeclareType
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
import orc.ast.xml.type.Type;
import orc.error.compiletime.CompilationException;

/**
 * Introduce a type alias.
 * 
 * @author quark
 */
public class DeclareType extends Expression {
	@XmlElement(required = true)
	public Type type;
	@XmlElement(required = true)
	public Expression body;

	public DeclareType() {
	}

	public DeclareType(final Type type, final Expression body) {
		this.type = type;
		this.body = body;
	}

	@Override
	public String toString() {
		return super.toString() + "(type " + type + " in " + body + ")";
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.DeclareType(type.unmarshal(), body.unmarshal(config));
	}
}
