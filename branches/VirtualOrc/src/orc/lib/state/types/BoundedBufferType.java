//
// BoundedBufferType.java -- Java class BoundedBufferType
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

package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.ListType;
import orc.type.tycon.MutableContainerType;

public class BoundedBufferType extends MutableContainerType {

	@Override
	public String toString() {
		return "BoundedBuffer";
	}

	@Override
	public Type makeCallableInstance(final List<Type> params) throws TypeException {
		/* We know that Buffer has exactly one type parameter */
		final Type T = params.get(0);

		final DotType dt = new DotType(/* no default behavior */);
		dt.addField("get", new ArrowType(T));
		dt.addField("getnb", new ArrowType(T));
		dt.addField("put", new ArrowType(T, Type.SIGNAL));
		dt.addField("putnb", new ArrowType(T, Type.SIGNAL));
		dt.addField("close", new ArrowType(Type.SIGNAL));
		dt.addField("closenb", new ArrowType(Type.SIGNAL));
		dt.addField("isClosed", new ArrowType(Type.BOOLEAN));
		dt.addField("getOpen", new ArrowType(Type.INTEGER));
		dt.addField("getBound", new ArrowType(Type.INTEGER));
		dt.addField("getAll", new ArrowType(ListType.listOf(T)));
		return dt;
	}

}
