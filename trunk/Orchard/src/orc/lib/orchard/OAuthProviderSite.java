//
// OAuthProviderSite.java -- Java class OAuthProviderSite
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.lib.orchard.Redirect.Redirectable;
import orc.oauth.OAuthProvider;
import orc.orchard.OrchardOAuthServlet;
import orc.orchard.Job.JobEngine;
import orc.values.sites.compatibility.Args;
import orc.OrcRuntime;
import orc.Handle;
import orc.values.sites.compatibility.SiteAdaptor;

public class OAuthProviderSite extends SiteAdaptor {
	public static class PendingOAuthAccessor {
		public OAuthAccessor accessor;
		public LinkedBlockingQueue<OAuthAccessor> ready;
	}

	/**
	 * This provider uses Java UI stuff to launch a browser
	 * and prompt the user to confirm authorization.
	 */
	public static class WebOAuthProvider extends OAuthProvider {
		private final JobEngine globals;
		private final Redirectable redirectable;

		public WebOAuthProvider(final JobEngine globals, final Redirectable redirectable, final String properties) throws IOException {
			super(properties);
			this.globals = globals;
			this.redirectable = redirectable;
		}

		@Override
		public OAuthAccessor authenticate(final String consumer, final List<OAuth.Parameter> request) throws Exception {
			final OAuthAccessor accessor = oauth.newAccessor(consumer);
			final LinkedBlockingQueue ready = new LinkedBlockingQueue();
			final String callbackURL = OrchardOAuthServlet.addToGlobalsAndGetCallbackURL(accessor, ready, globals);
			// get a request token
			oauth.obtainRequestToken(accessor, request, callbackURL);
			// request authorization and wait for response
			redirectable.redirect(oauth.getAuthorizationURL(accessor, callbackURL));
			ready.take();
			// get the access token
			oauth.obtainAccessToken(accessor);
			return accessor;
		}
	}

	@Override
	public void callSite(final Args args, final Handle caller) throws TokenException {
		final OrcRuntime engine = caller.runtime();
		if (!(engine instanceof JobEngine)) {
			throw new UnsupportedOperationException("This site is not supported on the engine " + engine.getClass().toString());
		}
		try {
			/**
			 * This implementation of OAuthProvider 
			 */
			caller.publish(new WebOAuthProvider((JobEngine) engine, (Redirectable) engine,
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
