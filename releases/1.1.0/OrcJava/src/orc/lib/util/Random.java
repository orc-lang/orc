//
// Random.java -- Java class Random
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

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

public class Random extends PartialSite {

	java.util.Random rnd;

	public Random() {
		rnd = new java.util.Random();
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		if (args.size() == 0) {
			return rnd.nextInt();
		}

		final int limit = args.intArg(0);

		if (limit > 0) {
			return rnd.nextInt(limit);
		} else {
			return null;
		}
	}

	@Override
	public Type type() {
		return new MultiType(new ArrowType(Type.INTEGER), new ArrowType(Type.INTEGER, Type.INTEGER));
	}

}
