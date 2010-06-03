//
// DotType.java -- Java class DotType
// Project OrcJava
//
// $Id: DotType.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.sites.compatibility.type.structured;

import java.util.Map;
import java.util.TreeMap;

import orc.sites.compatibility.type.Type;

/**
 * Composite type for sites which can receive messages (using the . notation)
 * 
 * A DotType is created with an optional default type (to be used when the
 * site is called with something other than a message), and then type entries
 * for each understood message are added using addField.
 * 
 * @author dkitchin
 */
@SuppressWarnings("hiding")
public class DotType extends Type {

	public static final Type NODEFAULT = new NoDefaultType();
	Type defaultType;
	Map<String, Type> fieldMap;

	public DotType() {
		this.defaultType = NODEFAULT;
		fieldMap = new TreeMap<String, Type>();
	}

	public DotType(final Type defaultType) {
		this.defaultType = defaultType;
		fieldMap = new TreeMap<String, Type>();
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();
		String sep = "";

		if (!(defaultType instanceof NoDefaultType)) {
			s.append('(');
			s.append(defaultType);
			s.append(" & ");
		}
		s.append('{');
		for (final String f : fieldMap.keySet()) {
			s.append(sep);
			sep = ", ";
			s.append(f + " :: ");
			s.append(fieldMap.get(f));
		}
		s.append('}');
		if (!(defaultType instanceof NoDefaultType)) {
			s.append(')');
		}

		return s.toString();
	}

}

final class NoDefaultType extends Type {

	@Override
	public String toString() {
		return "no_default_type";
	}
}
