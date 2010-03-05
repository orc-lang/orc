//
// OAuthProviderSite.java -- Java class OAuthProviderSite
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;

import kilim.Mailbox;
import kilim.Pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import orc.error.runtime.JavaException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.lib.orchard.Redirect.Redirectable;
import orc.oauth.OAuthProvider;
import orc.orchard.OrchardOAuthServlet;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;

public class OAuthProviderSite extends Site {
	public static class PendingOAuthAccessor {
		public OAuthAccessor accessor;
		public Mailbox<OAuthAccessor> ready;
	}

	/**
	 * This provider uses Java UI stuff to launch a browser
	 * and prompt the user to confirm authorization.
	 */
	public static class WebOAuthProvider extends OAuthProvider {
		private final OrcEngine globals;
		private final Redirectable redirectable;

		public WebOAuthProvider(final OrcEngine globals, final Redirectable redirectable, final String properties) throws IOException {
			super(properties);
			this.globals = globals;
			this.redirectable = redirectable;
		}

		@Override
		public OAuthAccessor authenticate(final String consumer, final List<OAuth.Parameter> request) throws Pausable, Exception {
			final OAuthAccessor accessor = oauth.newAccessor(consumer);
			final Mailbox ready = new Mailbox();
			final String callbackURL = OrchardOAuthServlet.addToGlobalsAndGetCallbackURL(accessor, ready, globals);
			// get a request token
			Kilim.runThreaded(new Callable() {
				public Object call() throws Exception {
					oauth.obtainRequestToken(accessor, request, callbackURL);
					return Kilim.signal;
				}
			});
			// request authorization and wait for response
			redirectable.redirect(oauth.getAuthorizationURL(accessor, callbackURL));
			ready.get();
			// get the access token
			Kilim.runThreaded(new Callable() {
				public Object call() throws Exception {
					oauth.obtainAccessToken(accessor);
					return Kilim.signal;
				}
			});
			return accessor;
		}
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		final OrcEngine engine = caller.getEngine();
		if (!(engine instanceof Redirectable)) {
			throw new SiteException("This site is not supported on the engine " + engine.getClass().toString());
		}
		try {
			/**
			 * This implementation of OAuthProvider 
			 */
			caller.resume(new WebOAuthProvider(engine, (Redirectable) engine,
			// force root-relative resource path
					"/" + args.stringArg(0)));
		} catch (final IOException e) {
			throw new JavaException(e);
		}
	}

	@SuppressWarnings("unused")
	private static class MockRedirectable implements Redirectable {
		public void redirect(final URL url) {
			System.out.println(url.toExternalForm());
		}
	}
}
