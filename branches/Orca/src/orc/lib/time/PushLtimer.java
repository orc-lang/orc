//
// PushLtimer.java -- Java class PushLtimer
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

package orc.lib.time;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class PushLtimer extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		caller.pushLtimer();
		caller.resume();
	}

	@Override
	public Type type() {
		return new ArrowType(Type.SIGNAL);
	}
}
