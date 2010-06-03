//
// EllipsisArrowType.java -- Java class EllipsisArrowType
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

package orc.values.sites.compatibility.type.structured;

import orc.values.sites.compatibility.type.Type;

@SuppressWarnings("hiding")
public class EllipsisArrowType extends Type {

	public Type repeatedArgType;
	public Type resultType;

	public EllipsisArrowType(final Type repeatedArgType, final Type resultType) {
		this.repeatedArgType = repeatedArgType;
		this.resultType = resultType;
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');
		s.append(repeatedArgType);
		s.append("... -> ");
		s.append(resultType);
		s.append(')');

		return s.toString();
	}

}
