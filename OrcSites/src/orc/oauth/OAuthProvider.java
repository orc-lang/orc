//
// OAuthProvider.java -- Java class OAuthProvider
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.oauth;

import java.io.IOException;
import java.security.PrivateKey;

import orc.values.sites.compatibility.DotSite;

import net.oauth.OAuthException;
import net.oauth.signature.RSA_SHA1;

/**
 * Abstract out the details of OAuth authentication so
 * that sites can use it without relying on a particular
 * platform (UI or web).
 *
 * @author quark
 */
public abstract class OAuthProvider extends DotSite {
	protected SimpleOAuth oauth;

	public OAuthProvider(final String properties) throws IOException {
		oauth = new SimpleOAuth(properties);
	}

//	public OAuthAccessor authenticate(final String name) throws Exception {
//		return authenticate(name, OAuth.newList());
//	}

//	/**
//	 * Get an authenticated OAuthAccessor.
//	 *
//	 * @param name
//	 * @param request
//	 * @return
//	 * @throws Exception
//	 */
//	public OAuthAccessor authenticate(final String name, final List<OAuth.Parameter> request) throws Exception {
//		throw new AssertionError("Must override OAuthProvider#authenticate(String)");
//	}

//	public final OAuthConsumer getConsumer(final String name) throws OAuthException {
//		return oauth.getConsumer(name);
//	}

	public final PrivateKey getPrivateKey(final String consumer) throws OAuthException {
		return (PrivateKey) oauth.getConsumer(consumer).getProperty(RSA_SHA1.PRIVATE_KEY);
	}
}
