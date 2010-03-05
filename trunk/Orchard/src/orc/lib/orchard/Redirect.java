//
// Redirect.java -- Java interface Redirect
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import java.net.MalformedURLException;
import java.net.URL;

import orc.error.runtime.JavaException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * Redirect the user to a URL.
 * @author quark
 */
public class Redirect extends Site {
	/**
	 * Interface implemented by an engine which can handle
	 * this site.
	 * @author quark
	 */
	public interface Redirectable {
		public void redirect(URL url);
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		final OrcEngine engine = caller.getEngine();
		final String url = args.stringArg(0);
		if (!(engine instanceof Redirectable)) {
			caller.error(new SiteException("This Orc engine does not support the Redirect site."));
		}
		try {
			((Redirectable) engine).redirect(new URL(url));
			caller.resume(signal());
		} catch (final MalformedURLException e) {
			caller.error(new JavaException(e));
		}
	}
}
