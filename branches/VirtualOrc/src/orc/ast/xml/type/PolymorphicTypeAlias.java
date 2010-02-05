//
// PolymorphicTypeAlias.java -- Java class PolymorphicTypeAlias
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

package orc.ast.xml.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * A syntactic type representing an aliased type with type parameters.
 * @author quark
 */
public class PolymorphicTypeAlias extends Type {
	@XmlElement(required = true)
	public Type type;
	@XmlAttribute(required = true)
	public int arity;

	public PolymorphicTypeAlias() {
	}

	public PolymorphicTypeAlias(final Type type, final int arity) {
		this.type = type;
		this.arity = arity;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.PolymorphicTypeAlias(type.unmarshal(), arity);
	}
}
