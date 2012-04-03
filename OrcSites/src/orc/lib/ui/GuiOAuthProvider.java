//
// GuiOAuthProvider.java -- Java class GuiOAuthProvider
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

package orc.lib.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JOptionPane;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuth.Parameter;
import orc.Handle;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.oauth.OAuthProvider;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * This provider uses Java UI stuff to launch a browser
 * and prompt the user to confirm authorization.
 */
public class GuiOAuthProvider extends OAuthProvider {
	public GuiOAuthProvider(final String properties) throws IOException {
		super(properties);
	}

	@Override
	protected void addMembers() {
		addMember("authenticate", new SiteAdaptor() {
			@Override
			public void callSite(final Args args, final Handle caller) throws TokenException {
				try {
					if (args.size() != 2) {
						throw new ArityMismatchException(2, args.size());
					}
					final String consumer = args.stringArg(0);
					final List<OAuth.Parameter> request = OAuth.newList();
			        for (int p = 1; p + 1 < args.size(); p += 2) {
			        	request.add(new Parameter(args.stringArg(p), args.stringArg(p + 1)));
			        }
					final OAuthAccessor accessor = oauth.newAccessor(consumer);
					oauth.obtainRequestToken(accessor, request, null);
					// prompt the user for authorization;
					// do not provide a callback URL
					final String authURL = oauth.getAuthorizationURL(accessor, null).toExternalForm();
					Desktop.getDesktop().browse(new URI(authURL));
					final int ok = JOptionPane.showConfirmDialog(null, "Your browser should open and ask you to" + " confirm authorization.\n\nPlease click Yes once" + " you have confirmed authorization.");
					if (ok != 0) {
						throw new OAuthException("Authorization refused by user.");
					}
					// confirm authorization
					oauth.obtainAccessToken(accessor);
					caller.publish(accessor);
				} catch (final IOException e) {
					throw new JavaException(e);
				} catch (final OAuthException e) {
					throw new JavaException(e);
				} catch (URISyntaxException e) {
					throw new JavaException(e);
				}
			}
		});
	}
}
