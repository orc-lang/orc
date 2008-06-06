package orc.orchard.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import orc.orchard.error.InvalidOilException;
import orc.orchard.error.QuotaException;
import orc.orchard.error.UnsupportedFeatureException;
import orc.orchard.interfaces.Oil;

public class ExecutorService extends UnicastRemoteObject implements
		orc.orchard.interfaces.ExecutorService
{
	private URI baseURI;
	private Map<URI, JobService> jobs = new HashMap();
	private Logger logger;
	
	public ExecutorService(URI baseURI, Logger logger) throws RemoteException, MalformedURLException {
		super();
		this.baseURI = baseURI;
		this.logger = logger;
		logger.info("Binding to '" + baseURI + "'");
		Naming.rebind(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}

	public ExecutorService(URI baseURI) throws RemoteException, MalformedURLException {
		this(baseURI, getDefaultLogger());
	}

	public URI submit(Oil program) throws QuotaException, InvalidOilException, RemoteException {
		try {
			return submit(program, new JobConfiguration());
		} catch (UnsupportedFeatureException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
	}

	public URI submit(Oil program, orc.orchard.interfaces.JobConfiguration configuration)
		throws QuotaException, InvalidOilException,	UnsupportedFeatureException, RemoteException
	{
		logger.info("submit");
		// garbage collect dead jobs
		reapJobs();
		// validate configuration
		if (configuration.getDebuggable()) {
			throw new UnsupportedFeatureException("Debuggable jobs not supported yet.");
		}
		if ("Java RMI".equals(configuration.getProtocol())) {
			JobService service = new JobService(configuration, program.getExpression(), logger);
			URI out;
			try {
				// FIXME: UIDs are not really unguessable like they should be
				out = new URI(this.baseURI + "/" + new UID());
				logger.info("Binding '" + out + "'");
				Naming.rebind(out.toString(), service);
				logger.info("Bound '" + out + "'");
			} catch (MalformedURLException e) {
				// this is impossible by construction
				throw new AssertionError(e);
			} catch (URISyntaxException e) {
				// this is impossible by construction
				throw new AssertionError(e);
			}
			jobs.put(out, service);
			return out;
		} else {
			throw new UnsupportedFeatureException("Protocol '"+configuration.getProtocol()+"' not supported.");
		}
	}

	public Set<URI> jobs() throws UnsupportedFeatureException, RemoteException {
		// it is necessary to wrap this in a HashSet so it's serializable
		return new HashSet(jobs.keySet());
	}
	
	private void reapJobs() throws RemoteException {
		for (Map.Entry<URI, JobService> job : jobs.entrySet()) {
			if (job.getValue().isDead()) {
				logger.info("Unbinding '" + job.getKey() + "'");
				try {
					Naming.unbind(job.getKey().toString());
				} catch (RemoteException e) {
					// do nothing, this isn't so terrible
				} catch (MalformedURLException e) {
					// this is impossible by construction
					throw new AssertionError(e);
				} catch (NotBoundException e) {
					// this is impossible by construction
					throw new AssertionError(e);
				}
				jobs.remove(job.getKey());
			}
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
	
	private static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(ExecutorService.class.toString());
		return out;
	}
}