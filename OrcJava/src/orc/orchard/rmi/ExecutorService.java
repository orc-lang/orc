package orc.orchard.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import orc.orchard.InvalidOilException;
import orc.orchard.JobConfiguration;
import orc.orchard.QuotaException;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.AbstractExecutorService;
import orc.orchard.oil.Oil;

public class ExecutorService extends AbstractExecutorService
	implements ExecutorServiceInterface
{
	private URI baseURI;
	
	public ExecutorService(URI baseURI, Logger logger) throws RemoteException, MalformedURLException {
		super(logger);
		this.baseURI = baseURI;
		logger.info("Binding to '" + baseURI + "'");
		UnicastRemoteObject.exportObject(this, 0);
		Naming.rebind(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}

	public ExecutorService(URI baseURI) throws RemoteException, MalformedURLException {
		this(baseURI, getDefaultLogger());
	}
	
	protected JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration("Java-RMI");
	}

	public URI submitConfigured(Oil program, JobConfiguration configuration)
		throws QuotaException, InvalidOilException,	UnsupportedFeatureException, RemoteException
	{
		logger.info("submit");
		// validate configuration
		if (configuration.getDebuggable()) {
			throw new UnsupportedFeatureException("Debuggable jobs not supported yet.");
		}
		try {
			URI out = new URI(this.baseURI + "/" + jobID());
			JobService service = new JobService(out, logger, configuration, program.unmarshal());
			return out;
		} catch (MalformedURLException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		} catch (URISyntaxException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
	}

	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("rmi://localhost/orchard");
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
			new ExecutorService(baseURI);
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + args[0] + "'");
			return;
		}
	}
}