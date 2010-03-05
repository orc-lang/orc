//
// SyncChannelType.java -- Java class SyncChannelType
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

public class SyncChannelType extends MutableContainerType {

	@Override
	public String toString() {
		return "SyncChannel";
	}

	@Override
	public Type makeCallableInstance(final List<Type> params) throws TypeException {
		/* We know that SyncChannel has exactly one type parameter */
		final Type T = params.get(0);

		final DotType dt = new DotType(/* no default behavior */);
		dt.addField("get", new ArrowType(T));
		dt.addField("put", new ArrowType(T, Type.SIGNAL));
		return dt;
	}

}
