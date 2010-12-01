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

import orc.TokenAPI;
import orc.error.runtime.TokenException;
import orc.values.Format;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.values.sites.compatibility.Types;
import orc.values.sites.TypedSite;
import orc.types.Type;


/**
 * Print arguments, converted to strings, in sequence.
 * 
 * @author dkitchin
 */
public class Print extends SiteAdaptor implements TypedSite {
	@Override
	public void callSite(final Args args, final TokenAPI caller) throws TokenException {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			Object arg = args.getArg(i);
			if (arg instanceof String) {
				sb.append((String) arg);
			} else {
				sb.append(Format.formatValueR(arg));
			}
		}
		caller.notify(new orc.PrintedEvent(sb.toString()));
		caller.publish(signal());
	}

	@Override
	public Type orcType() {
		return Types.function(Types.top(), Types.signal());
	}
}
