//
// ArrayType.java -- Java class ArrayType
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
import orc.type.tycon.MutableContainerType;

public class ArrayType extends MutableContainerType {

	@Override
	public String toString() {
		return "Array";
	}

	@Override
	public Type makeCallableInstance(final List<Type> params) throws TypeException {
		final Type T = params.get(0);

		/* Default behavior is element reference retrieval */
		final Type RefOfT = new RefType().instance(T);
		final DotType arrayType = new DotType(new ArrowType(Type.INTEGER, RefOfT));

		final Type ArrayOfT = new ArrayType().instance(T);
		arrayType.addField("get", new ArrowType(Type.INTEGER, T));
		arrayType.addField("set", new ArrowType(Type.INTEGER, T, Type.SIGNAL));
		arrayType.addField("slice", new ArrowType(Type.INTEGER, Type.INTEGER, ArrayOfT));
		arrayType.addField("length", new ArrowType(Type.INTEGER));
		arrayType.addField("fill", new ArrowType(T, Type.SIGNAL));
		return arrayType;
	}

}
