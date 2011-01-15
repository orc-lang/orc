//
// Redirect.java -- Java interface Redirect
// Project Orchard
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import java.net.MalformedURLException;
import java.net.URL;

import orc.Handle;
import orc.OrcRuntime;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.run.Orc;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Redirect the user to a URL.
 * @author quark
 */
public class Redirect extends SiteAdaptor {
	/**
	 * Interface implemented by an engine which can handle
	 * this site.
	 * @author quark
	 */
	public interface Redirectable {
		public void redirect(URL url);
	}

	@Override
	public void callSite(final Args args, final Handle caller) throws TokenException {
		final OrcRuntime engine = ((Orc.Token) caller).runtime(); //FIXME:Use OrcEvents, not subclassing for Redirects
		final String url = args.stringArg(0);
		if (!(engine instanceof Redirectable)) {
			caller.$bang$bang(new JavaException(new UnsupportedOperationException("This Orc engine does not support the Redirect site.")));
		}
		try {
			((Redirectable) engine).redirect(new URL(url));
			caller.publish(signal());
		} catch (final MalformedURLException e) {
			caller.$bang$bang(new JavaException(e));
		}
	}
}
