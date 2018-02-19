//
// Orchard.java -- Java class Orchard
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.http.spi.JettyHttpServerProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Run this from the command line to start a standalone Orchard server.
 *
 * @author quark
 */
public class Orchard {
    private static Logger orchardLogger;

    public static void main(final String args[]) throws Exception {
        setupLogging();

        int port;
        if (args.length == 0) {
            port = 8081;
        } else if (args.length == 1) {
            if (args[0].equals("--help") || args[0].equals("-help") || args[0].equals("-?")) {
                printUsage();
                return;
            } else {
                try {
                    port = Integer.parseInt(args[0]);
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
    }

    protected static void printUsage() {
        System.err.println("Usage: ... [<port number>]");
        System.exit(1);
    }

    protected static void setupLogging() throws SecurityException {
        // For consistency, point Jetty logging at java.util.log
        org.eclipse.jetty.util.log.Log.setLog(new JavaUtilLog());

        // Now, set up java.util.log
        for (final java.util.logging.Handler handler : Logger.getLogger("").getHandlers()) {
            if (handler instanceof java.util.logging.ConsoleHandler) {
                handler.setLevel(Level.ALL);
                handler.setFormatter(new SyslogishFormatter());
            }
        }
        orchardLogger = Logger.getLogger("orc.orchard");
        orchardLogger.setLevel(Level.FINER);
    }

    protected static void startJetty(final int port) throws URISyntaxException, Exception {
        final Server server = new Server();
        JettyHttpServerProvider.setServer(server);

        final ServerConnector connector = new ServerConnector(server);
        connector.setHost("localhost");
        connector.setPort(port);
        server.addConnector(connector);

        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        final HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[] { contexts, new DefaultHandler() });
        server.setHandler(handlerCollection);

        // Assumption: WAR is located in the same location this as this class.
        final URI warLocation = OrchardDemo.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        final WebAppContext webappContext = new WebAppContext(contexts, warLocation.resolve("orchard.war").getPath(), "/orchard");

        webappContext.setInitParameter("orc.lib.orchard.forms.url", "http://localhost:" + port + "/orchard/FormsServlet");

        server.setStopAtShutdown(true);
        server.start();
    }
}
