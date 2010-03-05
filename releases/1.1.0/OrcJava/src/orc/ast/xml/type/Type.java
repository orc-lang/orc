//
// Type.java -- Java class Type
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

import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * @author quark, dkitchin
 */
@XmlSeeAlso(value = { ArrowType.class, ClassnameType.class, Datatype.class, PolymorphicTypeAlias.class, SiteType.class, Top.class, TupleType.class, TypeApplication.class, TypeVariable.class })
public abstract class Type {
	/** Convert this syntactic type into an actual type.
	 * @return A new node.
	 */
	public abstract orc.ast.oil.type.Type unmarshal();

	public static List<orc.ast.oil.type.Type> unmarshalAll(final Type[] ts) {

		if (ts == null) {
			return null;
		}

		final List<orc.ast.oil.type.Type> newts = new LinkedList<orc.ast.oil.type.Type>();
		for (final Type t : ts) {
			newts.add(t.unmarshal());
		}
		return newts;
	}
}
