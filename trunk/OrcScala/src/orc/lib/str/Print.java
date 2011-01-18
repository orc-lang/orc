//
// Print.java -- Java class Print
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

package orc.lib.str;

import orc.Handle;
import orc.error.runtime.TokenException;
import orc.types.Type;
import orc.values.Format;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.values.sites.compatibility.Types;

/**
 * Print arguments, converted to strings, in sequence.
 * 
 * @author dkitchin
 */
public class Print extends SiteAdaptor implements TypedSite {
	@Override
	public void callSite(final Args args, final Handle caller) throws TokenException {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			final Object arg = args.getArg(i);
			if (arg instanceof String) {
				sb.append((String) arg);
			} else {
				sb.append(Format.formatValueR(arg));
			}
		}
		caller.notifyOrc(new orc.PrintedEvent(sb.toString()));
		caller.publish(signal());
	}

	@Override
	public Type orcType() {
		return Types.function(Types.top(), Types.signal());
	}
}
