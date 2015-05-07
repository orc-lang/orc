package orc;

import orc.orchard.OrchardProperties;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * Run this from the command line to start a standalone Orchard server.
 * @author quark
 */
public class OrchardDemo {
	private static void printUsage() {
		System.err.println("Usage: ... [<port number>]");
		System.exit(1);
	}
	public static void main(String args[]) throws Exception {
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
				} catch (NumberFormatException _) {
					printUsage();
					return;
				}
			}
		} else {
			printUsage();
			return;
		}
		// set reasonable defaults for a demo
		OrchardProperties.setProperty("orc.lib.orchard.forms.url",
				"http://localhost:"+PORT+"/orchard/FormsServlet");
		OrchardProperties.setProperty("orc.orchard.Accounts.url", "");
		OrchardProperties.setProperty("orc.orchard.GuestAccount.canImportJava", "true");
        Server server = new Server();
        Connector connector = new SelectChannelConnector();
        connector.setPort(PORT);
        connector.setHost("127.0.0.1");
        server.addConnector(connector);
        
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);
        new WebAppContext(contexts,
        		OrchardDemo.class.getResource("/web.war").toExternalForm(),
        		"/orchard");
        new WebAppContext(contexts,
        		OrchardDemo.class.getResource("/root-demo.war").toExternalForm(),
        		"/");
        
        server.setStopAtShutdown(true);
        server.start();
        BareBonesBrowserLaunch.openURL("http://localhost:" + PORT + "/demo.shtml");
    }
}
