//
// ListType.java -- Java class ListType
// Project OrcJava
//
// $Id: ListType.java 1553 2010-02-26 17:02:05Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.sites.compatibility.type.structured;

import orc.error.compiletime.typing.TypeException;
import orc.sites.compatibility.type.Type;

public class ListType extends Type {

	@Override
	public String toString() {
		return "List";
	}

	public static Type listOf(final Type T) throws TypeException {
		return new ListType();//.instance(T);
	}

}
