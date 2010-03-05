//
// Error.java -- Java class Error
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

package orc.runtime.sites.core;

import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class Error extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		caller.error(new SiteException(args.stringArg(0)));
	}

	@Override
	public Type type() {
		return new ArrowType(Type.STRING, Type.BOT);
	}
}
