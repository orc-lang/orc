//
// Print.java -- Java class Print
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

package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.structured.EllipsisArrowType;

/**
 * Print arguments, converted to strings, in sequence.
 *
 * @author dkitchin
 */
public class Print extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			sb.append(String.valueOf(args.getArg(i)));
		}
		caller.print(sb.toString(), false);
		caller.resume(Value.signal());
	}

	@Override
	public Type type() {
		return new EllipsisArrowType(Type.TOP, Type.SIGNAL);
	}
}
