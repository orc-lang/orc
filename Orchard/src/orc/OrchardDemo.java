//
// OrchardDemo.java -- Java class OrchardDemo
// Project Orchard
//
// $Id$
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
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
 * Run this from the command line to start a standalone Orchard demonstration.
 * @author quark
 */
public class OrchardDemo {
	private static Logger orchardLogger;

	public static void main(final String args[]) throws Exception {
		setupLogging();

		int port;
		if (args.length == 0) {
			port = 8080;
		} else if (args.length == 1) {
			if (args[0].equals("--help") || args[0].equals("-help") || args[0].equals("-?")) {
				printUsage();
				return;
			} else {
				try {
					port = Integer.valueOf(args[0]);
				} catch (final NumberFormatException nfe) {
					printUsage();
					return;
				}
			}
		} else {
			printUsage();
			return;
		}

		// System.setProperty("DEBUG", "true");

		startJetty(port);

		Desktop.getDesktop().browse(new URI("http://localhost:" + port + "/demo.shtml"));
	}

	protected static void printUsage() {
		System.err.println("Usage: ... [<port number>]");
		System.exit(1);
	}

	protected static void setupLogging() throws SecurityException {
		// Consistent syslog-ish logging format for JUL and Jetty
		for (final java.util.logging.Handler handler : Logger.getLogger("").getHandlers()) {
			if (handler instanceof java.util.logging.ConsoleHandler) {
				handler.setLevel(Level.ALL);
				handler.setFormatter(new SyslogishFormatter());
			}
		}
		orchardLogger = Logger.getLogger("orc.orchard");
		orchardLogger.setLevel(Level.FINER);
		org.mortbay.log.Log.setLog(new SyslogishJettyLogger());
	}

	@SuppressWarnings("unused")
	protected static void startJetty(final int port) throws URISyntaxException, Exception {
		final Server server = new Server();
		JettyHttpServerProvider.setServer(server);

		final Connector connector = new SelectChannelConnector();
		connector.setHost("localhost");
		connector.setPort(port);
		server.addConnector(connector);

		final ContextHandlerCollection contexts = new ContextHandlerCollection();
		final HandlerCollection handlerCollection = new HandlerCollection();
		handlerCollection.setHandlers(new Handler[] { contexts, new DefaultHandler() });
		server.setHandler(handlerCollection);

		// Assumption: WARs are located in the same location this as this class.
		final URI warLocation = OrchardDemo.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		final WebAppContext webappContext = new WebAppContext(contexts, warLocation.resolve("orchard.war").getPath(), "/orchard");

		// set reasonable defaults for a demo
		final Map<String, String> orchardInitParms = webappContext.getInitParams() != null ? webappContext.getInitParams() : new HashMap<String, String>();
		orchardInitParms.put("orc.lib.orchard.forms.url", "http://localhost:" + port + "/orchard/FormsServlet");
		orchardInitParms.put("orc.orchard.publishAddress", "http://localhost:" + port + "/orchard");
		orchardInitParms.put("orc.orchard.Accounts.url", "");
		orchardInitParms.put("orc.orchard.GuestAccount.canImportJava", "true");
		webappContext.setInitParams(orchardInitParms);

		new WebAppContext(contexts, warLocation.resolve("demo-root.war").getPath(), "/");

		server.setStopAtShutdown(true);
		server.start();
	}
}
