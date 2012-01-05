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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.j2se6.JettyHttpServerProvider;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Run this from the command line to start a standalone Orchard server.
 * @author quark
 */
public class Orchard {
	private static void printUsage() {
		System.err.println("Usage: ... [<port number>]");
		System.exit(1);
	}

	public static void main(final String args[]) throws Exception {
		final Logger orchardLogger = Logger.getLogger("orc.orchard");
		orchardLogger.setLevel(Level.FINER);
		final java.util.logging.ConsoleHandler handler = new java.util.logging.ConsoleHandler();
		handler.setLevel(Level.FINER);
		orchardLogger.addHandler(handler);

		int PORT;
		if (args.length == 0) {
			PORT = 8081;
		} else if (args.length == 1) {
			if (args[0].equals("--help") || args[0].equals("-help")) {
				printUsage();
				return;
			} else {
				try {
					PORT = Integer.valueOf(args[0]);
				} catch (final NumberFormatException _) {
					printUsage();
					return;
				}
			}
		} else {
			printUsage();
			return;
		}

		// System.setProperty("DEBUG", "true");

		final Server server = new Server();
		JettyHttpServerProvider.setServer(server);

		final Connector connector = new SelectChannelConnector();
		connector.setHost("localhost");
		connector.setPort(PORT);
		server.addConnector(connector);

		final ContextHandlerCollection contexts = new ContextHandlerCollection();
		final HandlerCollection handlerCollection = new HandlerCollection();
		handlerCollection.setHandlers(new Handler[] { contexts, new DefaultHandler() });
		server.setHandler(handlerCollection);

		// Assumption: WAR is located in the same location this as this class.
		final URI warLocation = OrchardDemo.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		final WebAppContext webappContext = new WebAppContext(contexts, warLocation.resolve("orchard.war").getPath(), "/orchard");

		final Map<String, String> orchardInitParms = webappContext.getInitParams() != null ? webappContext.getInitParams() : new HashMap<String, String>();
		orchardInitParms.put("orc.lib.orchard.forms.url", "http://localhost:" + PORT + "/orchard/FormsServlet");
		webappContext.setInitParams(orchardInitParms);

		server.setStopAtShutdown(true);
		server.start();
	}
}
