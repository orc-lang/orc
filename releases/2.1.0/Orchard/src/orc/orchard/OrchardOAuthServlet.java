//
// OrchardOAuthServlet.java -- Java class OrchardOAuthServlet
// Project Orchard
//
// $Id$
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;

public class OrchardOAuthServlet extends HttpServlet {
	public final static String MAILBOX = "orc.orchard.OrchardOAuthServlet.MAILBOX";
	protected static Logger logger = Logger.getLogger("orc.orchard.OrchardOAuthServlet");

	/**
	 * Adds this accessor to the Orc engine globals for retrieval when authorization callback is received.
	 * This is not idempotent, only call this method once per auth request.
	 *
	 * @param accessor Accessor to be authorized
	 * @param mbox Mailbox to be signaled when authorization is received
	 * @param job Orchard job that will receive the callback
	 * @return Callback URL string
	 * @throws IOException
	 */
	public static String addToGlobalsAndGetCallbackURL(final OAuthAccessor accessor, final LinkedBlockingQueue mbox, final Job job) throws IOException {
		accessor.setProperty(MAILBOX, mbox);
		final String key = AbstractExecutorService.globals.add(job, accessor);
		/* 'Twould be nice to figure out the callback URL automatically from
		 * the servlet context, but not possible with the Servlet API. */
		return OAuth.addParameters(accessor.consumer.callbackURL, "k", key);
	}

	public static final Object signal = new Object();

	/**
	 * Receive and validate an authorization callback and alert the client.
	 *
	 * @param request
	 * @throws IOException
	 * @throws OAuthException
	 * @throws InterruptedException
	 */
	public void receiveAuthorization(final HttpServletRequest request) throws IOException, OAuthException, InterruptedException {
		final OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
		requestMessage.requireParameters(OAuth.OAUTH_TOKEN, "k");

		final OAuthAccessor accessor = (OAuthAccessor) AbstractExecutorService.globals.remove(requestMessage.getParameter("k"));
		if (accessor == null) {
			return;
		}

		if (!accessor.requestToken.equalsIgnoreCase(requestMessage.getParameter(OAuth.OAUTH_TOKEN))) {
			logger.severe("OrchardOAuthServlet: token mismatch: received " + requestMessage.getParameter(OAuth.OAUTH_TOKEN) + ", but expected " + accessor.requestToken);
			throw new OAuthException("OrchardOAuthServlet: token mismatch");
		}

		final String verifier = requestMessage.getParameter("oauth_verifier");
		if (verifier != null) {
			accessor.setProperty("oauth_verifier", verifier);
		}

		final LinkedBlockingQueue mbox = (LinkedBlockingQueue) accessor.getProperty(MAILBOX);
		if (mbox == null) {
			return;
		}
		logger.fine("OrchardOAuthServlet: approving " + accessor.requestToken);
		mbox.put(signal);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		try {
			receiveAuthorization(request);
		} catch (final OAuthException e) {
			throw new ServletException(e);
		} catch (final InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();
		}
		final PrintWriter out = response.getWriter();
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		out.write("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
		out.write("<head>\n");
		out.write("<meta http-equiv=\"Content-Type\" content=\"text/xhtml+xml; charset=UTF-8\" />\n");
		out.write("<title>Authorization Received</title>\n");
		out.write("</head>\n");
		out.write("<body>\n");
		out.write("<h1>Authorization Received</h1>\n");
		out.write("<h2>Thank you, you may now close this window.</h2>\n");
		out.write("</body>\n");
		out.write("</html>\n");
		out.close();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#getServletInfo()
	 */
	@Override
	public String getServletInfo() {
		return OrchardProperties.getProperty("war.manifest.Implementation-Version") +
			" rev. " + OrchardProperties.getProperty("war.manifest.SVN-Revision") +
			"  Copyright The University of Texas at Austin";
	}

}
