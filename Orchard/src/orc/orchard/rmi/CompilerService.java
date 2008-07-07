package orc.orchard.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import orc.orchard.AbstractCompilerService;
import orc.orchard.api.CompilerServiceInterface;

public class CompilerService extends AbstractCompilerService
	implements CompilerServiceInterface
{	
	public CompilerService(URI baseURI, Logger logger) throws RemoteException, MalformedURLException {
		super(logger);
		logger.info("Binding to '" + baseURI + "'");
		UnicastRemoteObject.exportObject(this, 0);
		Naming.rebind(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}
	public CompilerService(URI baseURI) throws RemoteException, MalformedURLException {
		this(baseURI, getDefaultLogger());
	}
	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("rmi://localhost/orchard/compiler");
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