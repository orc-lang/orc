//
// JavaArray.java -- Java class JavaArray
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

package orc.lib.util;

import java.lang.reflect.Array;
import java.util.HashMap;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.ArrayType;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

public class JavaArray extends EvalSite {
	private static HashMap<String, Class> types = new HashMap<String, Class>();
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
		} else {
			final Class type = types.get(args.stringArg(1));
			if (type == null) {
				throw new SiteException("Unrecognized array element type: " + args.stringArg(0));
			}
			return Array.newInstance(type, args.intArg(0));
		}
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type ArrayOfX = new ArrayType().instance(X);
		return new MultiType(new ArrowType(Type.INTEGER, ArrayOfX, 1), new ArrowType(Type.INTEGER, Type.STRING, ArrayOfX, 1));
	}
}
