//
// Println.java -- Java class Println
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
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.values.sites.compatibility.type.Type;
import orc.values.sites.compatibility.type.structured.EllipsisArrowType;

/**
 * Print arguments, converted to strings, in sequence, each followed by
 * newlines.
 * 
 * @author dkitchin
 */
public class Println extends SiteAdaptor {
	@Override
	public void callSite(final Args args, final TokenAPI caller) throws TokenException {
		for (int i = 0; i < args.size(); i++) {
			caller.notify(new orc.PrintedEvent(String.valueOf(args.getArg(i)) + "\n"));
		}
		if (args.size() == 0) {
			caller.notify(new orc.PrintedEvent("\n"));
		}
		caller.publish(signal());
	}

	public Type type() {
		return new EllipsisArrowType(Type.TOP, Type.SIGNAL);
	}

}
