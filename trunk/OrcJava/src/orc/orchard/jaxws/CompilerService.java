package orc.orchard.jaxws;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.orchard.AbstractCompilerService;
import orc.orchard.JobConfiguration;

@WebService(endpointInterface="orc.orchard.jaxws.CompilerServiceInterface")
public class CompilerService extends AbstractCompilerService {
	/** Exists only to satisfy a silly requirement of JAX-WS */
	public CompilerService() {
		super(null);
		throw new AssertionError("Do not call this method directly");
	}
	
	public CompilerService(URI baseURI, Logger logger) throws RemoteException, MalformedURLException {
		super(logger);
		logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}

	public CompilerService(URI baseURI) throws RemoteException, MalformedURLException {
		this(baseURI, getDefaultLogger());
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
		try {
			new CompilerService(baseURI);
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + args[0] + "'");
			return;
		}
	}
}