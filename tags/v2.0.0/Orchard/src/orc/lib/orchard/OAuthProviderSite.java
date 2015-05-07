//
// OAuthProviderSite.java -- Java class OAuthProviderSite
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import orc.Handle;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.oauth.OAuthProvider;
import orc.orchard.Job;
import orc.orchard.OrchardOAuthServlet;
import orc.values.sites.compatibility.Args;
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
		private final Job job;
		private final Handle caller;

		public WebOAuthProvider(final Job job, final Handle caller, final String properties) throws IOException {
			super(properties);
			this.job = job;
			this.caller = caller;
		}

		@Override
		public OAuthAccessor authenticate(final String consumer, final List<OAuth.Parameter> request) throws Exception {
			final OAuthAccessor accessor = oauth.newAccessor(consumer);
			final LinkedBlockingQueue ready = new LinkedBlockingQueue();
			final String callbackURL = OrchardOAuthServlet.addToGlobalsAndGetCallbackURL(accessor, ready, job);
			// get a request token
			oauth.obtainRequestToken(accessor, request, callbackURL);
			// request authorization and wait for response
			caller.notifyOrc(new orc.lib.web.BrowseEvent(oauth.getAuthorizationURL(accessor, callbackURL)));
			ready.take();
			// get the access token
			oauth.obtainAccessToken(accessor);
			return accessor;
		}
	}

	@Override
	public void callSite(final Args args, final Handle caller) throws TokenException {
		try {
			/**
			 * This implementation of OAuthProvider 
			 */
			final Job job = Job.getJobFromHandle(caller);
			if (job == null) {
				caller.halt();
				return;
			}
			caller.publish(new WebOAuthProvider(job, caller,
			// force root-relative resource path
					"/" + args.stringArg(0)));
		} catch (final IOException e) {
			throw new JavaException(e);
		}
	}

}
