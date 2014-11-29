//
// LoginServlet.java -- Java class LoginServlet
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {
	private Connection db = null;
	protected static Logger logger = Logger.getLogger("orc.orchard.LoginServlet");

	public void initIfNeeded() {
		if (db == null) {
			try {
				final String accountsUrl = OrchardProperties.getProperty("orc.orchard.Accounts.url");
				if (accountsUrl.startsWith("jdbc:")) {
					db = DriverManager.getConnection(accountsUrl);
				}
			} catch (final SQLException e) {
				logger.log(Level.SEVERE, "LoginServlet: Orchard accounts database connection failed", e);
			}
		}
	}

	/** Return null if dev key not found. */
	public String getDevKey(final String username, final String password) throws SQLException {
		initIfNeeded();
		if (db == null) {
			return null;
		}
		final PreparedStatement query = db.prepareStatement("SELECT developer_key FROM account" + " WHERE username = ? AND password_md5 = md5(salt || ?)");
		try {
			query.setString(1, username);
			query.setString(2, password);
			final ResultSet result = query.executeQuery();
			try {
				if (!result.next()) {
					return null;
				} else {
					logger.info("LoginServlet: Login succeeded for " + username);
					return result.getString("developer_key");
				}
			} finally {
				result.close();
			}
		} finally {
			query.close();
		}
	}

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		if (!request.isSecure()) {
			logger.log(Level.WARNING, "LoginServlet: Login request over insecure channel, redirecting");
	        final StringBuffer url = new StringBuffer();
            url.append("https://");
            url.append(request.getServerName());
            /* NOTE: The port number is dropped here, because there is no mapping
             * for non-port 80 to a HTTPS port.  All redirects are to port 443. */
            url.append(request.getRequestURI());
			response.sendRedirect(response.encodeRedirectURL(url.toString()));
			return;
		}
		final String auth = request.getHeader("Authorization");
		if (auth != null) {
			final String up64 = auth.substring(6);
			final String up = new String(new sun.misc.BASE64Decoder().decodeBuffer(up64));
			final String[] ups = up.split(":", 2);
			try {
				final String devKey = getDevKey(ups[0], ups[1]);
				if (devKey != null && !devKey.isEmpty()) {
					/*FIXME: Vulnerable to XSRF and god knows what else. */
					Cookie keyCookie = new Cookie("OrchardDevKey", devKey);
					keyCookie.setMaxAge(-1); /* session cookie */
					keyCookie.setPath("/"); /* This is a violation of the RFCs, but works in practice */
					keyCookie.setSecure(true);
					response.addCookie(keyCookie);
					response.sendRedirect(response.encodeRedirectURL("/tryorc.shtml"));
					return;
				} else {
					logger.log(Level.SEVERE, "LoginServlet: SECURITY: Login failed for claimed username " + ups[0]);
				}
			} catch (final SQLException e) {
				logger.log(Level.SEVERE, "LoginServlet: Thrown during login attempt: username=" + ups[0], e);
			}
		}
		/* Since basic auth can't be logged out, users should remember to quit browser. */
		response.setHeader("WWW-Authenticate", "Basic realm=\"orc.csres.utexas.edu\"");
		response.setStatus(401);
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
