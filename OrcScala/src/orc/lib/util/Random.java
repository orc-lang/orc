//
// Random.java -- Java class Random
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

package orc.lib.util;

import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.PartialSite;
import orc.values.sites.compatibility.Types;

public class Random extends PartialSite implements TypedSite {

	java.util.Random rnd;

	public Random() {
		rnd = new java.util.Random();
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		if (args.size() == 0) {
			return BigInteger.valueOf(rnd.nextInt());
		}

		final int limit = args.intArg(0);

		if (limit > 0) {
			return BigInteger.valueOf(rnd.nextInt(limit));
		} else {
			return null;
		}
	}

	@Override
	public Type orcType() {
		return Types.overload(Types.function(Types.integer()), Types.function(Types.integer(), Types.integer()));
	}

}
