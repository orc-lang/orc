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

import javax.xml.ws.Endpoint;

import orc.orchard.OrchardProperties;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.j2se6.JettyHttpServerProvider;
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
		JettyHttpServerProvider.setServer(server);

		final Connector connector = new SelectChannelConnector();
		connector.setPort(PORT);
		connector.setHost("127.0.0.1");
		server.addConnector(connector);

		final HandlerCollection handlers = new HandlerCollection();
		server.setHandler(handlers);

		final ContextHandlerCollection contexts = new ContextHandlerCollection();
		handlers.addHandler(contexts);

		new WebAppContext(contexts, "./web", "/orchard");

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
	}
}
