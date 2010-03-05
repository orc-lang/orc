//
// TypeVariable.java -- Java class TypeVariable
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

/**
 * A simple named type.
 * 
 * @author dkitchin
 */
public class TypeVariable extends Type {
	@XmlAttribute(required = true)
	public int index;
	@XmlAttribute(required = false)
	public String name;

	public TypeVariable() {
	}

	public TypeVariable(final int index) {
		this(index, null);
	}

	public TypeVariable(final int index, final String name) {
		this.index = index;
		this.name = name;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.TypeVariable(index, name);
	}
}
