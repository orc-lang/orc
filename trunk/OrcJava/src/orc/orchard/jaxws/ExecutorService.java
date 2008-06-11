package orc.orchard.jaxws;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.orchard.JobConfiguration;
import orc.orchard.Oil;
import orc.orchard.error.InvalidOilException;
import orc.orchard.error.QuotaException;
import orc.orchard.error.UnsupportedFeatureException;


@WebService(endpointInterface="orc.orchard.jaxws.ExecutorServiceInterface")
public class ExecutorService extends orc.orchard.ExecutorService<Oil, JobConfiguration>
	implements ExecutorServiceInterface
{
	private URI baseURI;
	
	/** Exists only to satisfy a silly requirement of JAX-WS */
	public ExecutorService() {
		super(null);
		throw new AssertionError("Do not call this method directly");
	}
	
	protected JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration("JAX-WS");
	}
	
	public ExecutorService(URI baseURI, Logger logger) throws RemoteException, MalformedURLException {
		super(logger);
		this.baseURI = baseURI;
		logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}

	public ExecutorService(URI baseURI) throws RemoteException, MalformedURLException {
		this(baseURI, getDefaultLogger());
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
			JobService service = new JobService(out, logger, configuration, program.getExpression());
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
			baseURI = new URI("http://localhost:8080/orchard");
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