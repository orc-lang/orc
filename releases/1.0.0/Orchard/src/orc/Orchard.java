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
	public static void main(String args[]) throws Exception {
		OrchardProperties.setProperty("orc.lib.orchard.forms.url",
				"http://localhost:"+PORT+"/orchard/FormsServlet");
        Server server = new Server();
        Connector connector = new SelectChannelConnector();
        connector.setPort(PORT);
        connector.setHost("127.0.0.1");
        server.addConnector(connector);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/orchard");
        context.setWar("./web");
        server.setHandler(context);
        server.setStopAtShutdown(true);
        server.start();
    }
}