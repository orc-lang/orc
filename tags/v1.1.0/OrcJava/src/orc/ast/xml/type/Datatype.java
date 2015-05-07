//
// Datatype.java -- Java class Datatype
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

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Datatype extends Type {
	@XmlAttribute(required = true)
	public String name;
	@XmlElementWrapper(required = true)
	@XmlElement(name = "member", required = true)
	public Type[][] members;
	@XmlAttribute(required = true)
	public int arity;

	public Datatype() {
	}

	public Datatype(final String name, final Type[][] members, final int arity) {
		this.name = name;
		this.members = members;
		this.arity = arity;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {

		/* Reduce each constructor to a list of its argument types.
		 * The constructor names are used separately in the dynamic
		 * semantics to give a string representation for the constructed
		 * values.
		 */
		final List<List<orc.ast.oil.type.Type>> cs = new LinkedList<List<orc.ast.oil.type.Type>>();
		for (final Type[] con : members) {
			final List<orc.ast.oil.type.Type> ts = Type.unmarshalAll(con);
			cs.add(ts);
		}

		return new orc.ast.oil.type.Datatype(cs, arity, name);
	}

}
