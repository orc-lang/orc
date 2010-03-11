//
// TupleType.java -- Java class TupleType
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

import javax.xml.bind.annotation.XmlElement;

/**
 * A syntactic type tuple: (T,...,T)
 * 
 * @author quark, dkitchin
 */
public class TupleType extends Type {
	@XmlElement(name = "item", required = true)
	public Type[] items;

	public TupleType() {
	}

	public TupleType(final Type[] items) {
		this.items = items;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.TupleType(Type.unmarshalAll(items));
	}
}
