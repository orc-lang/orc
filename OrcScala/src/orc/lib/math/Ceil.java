//
// Ceil.java -- Java class Ceil
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

package orc.lib.math;

import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.Types;

public class Ceil extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		final Number n = args.numberArg(0);
		final int i = n.intValue();
		return Integer.valueOf(n.equals(Integer.valueOf(i)) ? i : i + 1);
	}

	public Type orcType() {
		return Types.function(Types.number(), Types.integer());
	}
}
