//
// OAuthProviderSite.java -- Java class OAuthProviderSite
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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
import net.oauth.OAuth.Parameter;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;

import orc.CallContext;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.oauth.OAuthProvider;
import orc.orchard.Job;
import orc.orchard.OrchardOAuthServlet;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;

public class OAuthProviderSite extends SiteAdaptor {
//    public static class PendingOAuthAccessor {
//        public OAuthAccessor accessor;
//        public LinkedBlockingQueue<OAuthAccessor> ready;
//    }

    /**
     * This provider uses Orc events to launch a browser and prompt the user to
     * confirm authorization.
     */
    public static class WebOAuthProvider extends OAuthProvider {
        private final Job job;

        public WebOAuthProvider(final Job job, final String properties) throws IOException {
            super(properties);
            this.job = job;
        }

        @Override
        protected void addMembers() {
            addMember("authenticate", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext caller) throws TokenException {
                    try {
                        final String consumer = args.stringArg(0);
                        final List<OAuth.Parameter> request = OAuth.newList();
                        for (int p = 1; p + 1 < args.size(); p += 2) {
                            request.add(new Parameter(args.stringArg(p), args.stringArg(p + 1)));
                        }
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
                        caller.publish(accessor);
                    } catch (final IOException e) {
                        throw new JavaException(e);
                    } catch (final OAuthException e) {
                        throw new JavaException(e);
                    } catch (final InterruptedException e) {
                        throw new JavaException(e);
                    }
                }
            });
        }
    }

    @Override
    public void callSite(final Args args, final CallContext caller) throws TokenException {
        try {
            /**
             * This implementation of OAuthProvider
             */
            final Job job = Job.getJobFromHandle(caller);
            if (job == null) {
                caller.halt();
                return;
            }
            caller.publish(new WebOAuthProvider(job,
            // force root-relative resource path
                    "/" + args.stringArg(0)));
        } catch (final IOException e) {
            throw new JavaException(e);
        }
    }

}
