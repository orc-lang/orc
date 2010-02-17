//
// OrchardDemo.java -- Java class OrchardDemo
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

import javax.xml.ws.Endpoint;

import orc.orchard.OrchardProperties;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.j2se6.JettyHttpServerProvider;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * Run this from the command line to start a standalone Orchard demonstration.
 * @author quark
 */
public class OrchardDemo {
	private static void printUsage() {
		System.err.println("Usage: ... [<port number>]");
		System.exit(1);
	}

	public static void main(final String args[]) throws Exception {
		int PORT;
		if (args.length == 0) {
			PORT = 8080;
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

		// set reasonable defaults for a demo
		OrchardProperties.setProperty("orc.lib.orchard.forms.url", "http://localhost:" + PORT + "/orchard/FormsServlet");
		OrchardProperties.setProperty("orc.orchard.Accounts.url", "");
		OrchardProperties.setProperty("orc.orchard.GuestAccount.canImportJava", "true");

		// Set JVM-wide HTTP server to Jetty.  JAX-WS (javax.xml.ws) uses this when publishing Endpoints.
		System.setProperty("com.sun.net.httpserver.HttpServerProvider", "org.mortbay.jetty.j2se6.JettyHttpServerProvider");

		final Server server = new Server();
		JettyHttpServerProvider.setServer(server);

		final Connector connector = new SelectChannelConnector();
		connector.setPort(PORT);
		connector.setHost("127.0.0.1");
		server.addConnector(connector);

		final HandlerCollection handlers = new HandlerCollection();
		server.setHandler(handlers);

		final ContextHandlerCollection contexts = new ContextHandlerCollection();
		handlers.addHandler(contexts);

		// Assumption: WARs are located in the same location this as this class.
		final URI warLocation = OrchardDemo.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		new WebAppContext(contexts, warLocation.resolve("web.war").getPath(), "/orchard");
		new WebAppContext(contexts, warLocation.resolve("root-demo.war").getPath(), "/");

		server.setStopAtShutdown(true);
		server.start();

		final Endpoint compilerSoapServiceEndpoint = Endpoint.create(new orc.orchard.soap.CompilerService());
		compilerSoapServiceEndpoint.publish("http://localhost:" + PORT + "/orchard/soap/compiler");
		final Endpoint executorSoapServiceEndpoint = Endpoint.create(new orc.orchard.soap.ExecutorService());
		executorSoapServiceEndpoint.publish("http://localhost:" + PORT + "/orchard/soap/executor");
		final Endpoint compilerJsonServiceEndpoint = Endpoint.create("https://jax-ws-commons.dev.java.net/json/", new orc.orchard.soap.CompilerService());
		compilerJsonServiceEndpoint.publish("http://localhost:" + PORT + "/orchard/json/compiler");
		final Endpoint executorJsonServiceEndpoint = Endpoint.create("https://jax-ws-commons.dev.java.net/json/", new orc.orchard.soap.ExecutorService());
		executorJsonServiceEndpoint.publish("http://localhost:" + PORT + "/orchard/json/executor");

		BareBonesBrowserLaunch.openURL("http://localhost:" + PORT + "/demo.shtml");
	}
}
