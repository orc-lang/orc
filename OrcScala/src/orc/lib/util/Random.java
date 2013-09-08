//
// Random.java -- Java class Random
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util;

import java.math.BigInteger;

import orc.error.runtime.ArityMismatchException;
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
		if (args.size() > 1) {
			throw new ArityMismatchException(1, args.size());
		}
		if (args.size() == 0) {
			return BigInteger.valueOf(rnd.nextInt());
		}

		if (!(args.getArg(0) instanceof Number) ||
		    args.longArg(0) > Integer.MAX_VALUE ||
		    args.longArg(0) <= 0L || ((Number) args.getArg(0)).doubleValue() > Integer.MAX_VALUE ||
		    ((Number) args.getArg(0)).doubleValue() <= 0.0 ||
		    ((Number) args.getArg(0)).doubleValue() != Math.rint(((Number) args.getArg(0)).doubleValue()) ) {
			throw new IllegalArgumentException("Random's argument must be an integer strictly between 0 and 2**31");
		}

		return BigInteger.valueOf(rnd.nextInt(args.intArg(0)));
	}

	@Override
	public Type orcType() {
		return Types.overload(Types.function(Types.integer()), Types.function(Types.integer(), Types.integer()));
	}

}
