//
// JavaArray.java -- Java class JavaArray
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.lang.reflect.Array;
import java.util.HashMap;

import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.BadArrayElementTypeException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.ArrayType;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

public class JavaArray extends EvalSite implements TypedSite {
	private static HashMap<String, Class<?>> types = new HashMap<String, Class<?>>();
	static {
		types.put("double", Double.TYPE);
		types.put("float", Float.TYPE);
		types.put("long", Long.TYPE);
		types.put("int", Integer.TYPE);
		types.put("short", Short.TYPE);
		types.put("byte", Byte.TYPE);
		types.put("char", Character.TYPE);
		types.put("boolean", Boolean.TYPE);
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		if (args.size() == 1) {
			return Array.newInstance(Object.class, args.intArg(0));
		} else if (args.size() == 2) {
			final Class<?> type = types.get(args.stringArg(1));
			if (type == null) {
				throw new BadArrayElementTypeException(args.stringArg(0));
			}
			return Array.newInstance(type, args.intArg(0));
		} else {
			throw new ArityMismatchException(2, args.size());
		}
	}

	@Override
	public Type orcType() {
		return ArrayType.getBuilder();
	}
}
