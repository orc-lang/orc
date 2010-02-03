//
// OrchardOAuthServlet.java -- Java class OrchardOAuthServlet
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

package orc.orchard;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kilim.Mailbox;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;
import orc.runtime.Kilim;
import orc.runtime.OrcEngine;

public class OrchardOAuthServlet extends HttpServlet {
	public final static String MAILBOX = "orc.orchard.OrchardOAuthServlet.MAILBOX";

	public static String getCallbackURL(final OAuthAccessor accessor, final Mailbox mbox, final OrcEngine globals) throws IOException {
		accessor.setProperty(MAILBOX, mbox);
		final String key = globals.addGlobal(accessor);
		// FIXME: we should figure out the callback URL
		// automatically from the servlet context
		return OAuth.addParameters(accessor.consumer.callbackURL, "k", key);
	}

	public void receiveAuthorization(final HttpServletRequest request) throws IOException, OAuthException {
		final OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
		requestMessage.requireParameters("oauth_token", "k");

		final OAuthAccessor accessor = (OAuthAccessor) OrcEngine.globals.remove(requestMessage.getParameter("k"));
		if (accessor == null) {
			return;
		}
		final Mailbox mbox = (Mailbox) accessor.getProperty(MAILBOX);
		if (mbox == null) {
			return;
		}
		System.out.println("OrchardOAuthServlet: approving " + accessor.requestToken);
		mbox.putb(Kilim.signal);
	}

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		try {
			receiveAuthorization(request);
		} catch (final OAuthException e) {
			throw new ServletException(e);
		}
		final PrintWriter out = response.getWriter();
		out.write("<html><head></head><body>");
		out.write("<h1>Thank you, you may now close this window.</h1>");
		out.write("</body></html>");
		out.close();
	}
}
