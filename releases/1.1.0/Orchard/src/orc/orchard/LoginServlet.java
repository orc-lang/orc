//
// LoginServlet.java -- Java class LoginServlet
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {
	private Connection db = null;

	public void initIfNeeded() {
		if (db == null) {
			try {
				final String accountsUrl = OrchardProperties.getProperty("orc.orchard.Accounts.url");
				if (accountsUrl.startsWith("jdbc:")) {
					db = DriverManager.getConnection(accountsUrl);
				}
			} catch (final SQLException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
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
		final String auth = request.getHeader("Authorization");
		if (auth != null) {
			final String up64 = auth.substring(6);
			final String up = new String(new sun.misc.BASE64Decoder().decodeBuffer(up64));
			final String[] ups = up.split(":", 2);
			try {
				final String devKey = getDevKey(ups[0], ups[1]);
				if (devKey != null) {
					response.sendRedirect(response.encodeRedirectURL("/tryorc.shtml?k=" + devKey));
					return;
				}
			} catch (final SQLException e) {
				e.printStackTrace();
			}
		}
		response.setHeader("WWW-Authenticate", "Basic realm=\"orc.csres.utexas.edu\"");
		response.setStatus(401);
	}
}
