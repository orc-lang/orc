//
// GuiOAuthProvider.java -- Java class GuiOAuthProvider
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.ui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;

import kilim.Pausable;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import orc.oauth.OAuthProvider;
import orc.runtime.Kilim;

import com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * This provider uses Java UI stuff to launch a browser
 * and prompt the user to confirm authorization.
 */
public class GuiOAuthProvider extends OAuthProvider {
	public GuiOAuthProvider(final String properties) throws IOException {
		super(properties);
	}

	@Override
	public OAuthAccessor authenticate(final String consumer, final List<OAuth.Parameter> request) throws Pausable, Exception {
		final OAuthAccessor accessor = oauth.newAccessor(consumer);
		Kilim.runThreaded(new Callable<Void>() {
			public Void call() throws Exception {
				oauth.obtainRequestToken(accessor, request, null);
				// prompt the user for authorization;
				// do not provide a callback URL
				final String authURL = oauth.getAuthorizationURL(accessor, null).toExternalForm();
				BareBonesBrowserLaunch.openURL(authURL);
				final int ok = JOptionPane.showConfirmDialog(null, "Your browser should open and ask you to" + " confirm authorization.\n\nPlease click Yes once" + " you have confirmed authorization.");
				if (ok != 0) {
					throw new OAuthException("Authorization refused by user.");
				}
				// confirm authorization
				oauth.obtainAccessToken(accessor);
				return null;
			}
		});
		return accessor;
	}
}
