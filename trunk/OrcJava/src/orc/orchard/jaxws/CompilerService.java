package orc.orchard.jaxws;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import orc.orchard.AbstractCompilerService;
import orc.orchard.InvalidProgramException;
import orc.orchard.JobConfiguration;
import orc.orchard.oil.Oil;

@WebService(endpointInterface="orc.orchard.jaxws.CompilerServiceInterface")
public class CompilerService extends AbstractCompilerService {
	/**
	 * Construct a service to run in an existing servlet context.
	 */
	public CompilerService() {
		super(getDefaultLogger());
	}
	
	/**
	 * Construct a service to run at a given URI using the standalone HTTP server.
	 */
	public CompilerService(Logger logger) {
		super(logger);
	}

	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("http://localhost:8080/orchardc");
		} catch (URISyntaxException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
		if (args.length > 0) {
			try {
				baseURI = new URI(args[0]);
			} catch (URISyntaxException e) {
				System.err.println("Invalid URI '" + args[0] + "'");
				return;
			}
		}
		CompilerService compiler = new CompilerService();
		compiler.logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), compiler);
		compiler.logger.info("Bound to '" + baseURI + "'");
	}
}