//
// HasType -- Java class HasType
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
import orc.ast.xml.type.Type;
import orc.error.compiletime.CompilationException;

/**
 * An expression with an ascribed type.
 * 
 * @author quark
 */
public class HasType extends Expression {
	@XmlElement(required = true)
	public Expression body;
	@XmlElement(required = true)
	public Type type;
	@XmlAttribute(required = true)
	public boolean checked;

	public HasType() {
	}

	public HasType(final Expression body, final Type type, final boolean checked) {
		this.body = body;
		this.type = type;
		this.checked = checked;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + body + " :: " + type + ")";
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		return new orc.ast.oil.expression.HasType(body.unmarshal(config), type.unmarshal(), checked);
	}
}
