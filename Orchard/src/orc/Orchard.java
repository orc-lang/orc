//
// Orchard.java -- Java class Orchard
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

package orc;

import orc.orchard.OrchardProperties;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Run this from the command line to start a standalone Orchard server.
 * @author quark
 */
public class Orchard {
	public static final int PORT = 8081;

	public static void main(final String args[]) throws Exception {
		OrchardProperties.setProperty("orc.lib.orchard.forms.url", "http://localhost:" + PORT + "/orchard/FormsServlet");
		final Server server = new Server();
		final Connector connector = new SelectChannelConnector();
		connector.setPort(PORT);
		connector.setHost("127.0.0.1");
		server.addConnector(connector);

		final WebAppContext context = new WebAppContext();
		context.setContextPath("/orchard");
		context.setWar("./web");
		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.start();
	}
}
