//
// MatchProxy.java -- Java class MatchProxy
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

package orc.runtime.sites.java;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;

public class MatchProxy extends PartialSite {

	public Class cls;

	public MatchProxy(final Class cls) {
		this.cls = cls;
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {

		/* Note: A match proxy will not match null, regardless of the class to be matched.
		 * Currently this is due to its implementation as a PartialSite, but more generally,
		 * since it is not possible to call null.getClass(), considering null to be of any
		 * particular class seems meaningless.
		 */

		final Object arg = args.getArg(0);

		if (cls.isAssignableFrom(arg.getClass())) {
			return arg;
		} else {
			return null;
		}
	}

}
