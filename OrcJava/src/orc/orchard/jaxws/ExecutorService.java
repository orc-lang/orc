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

import orc.orchard.InvalidOilException;
import orc.orchard.JobConfiguration;
import orc.orchard.QuotaException;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.oil.Oil;


@WebService(endpointInterface="orc.orchard.jaxws.ExecutorServiceInterface")
public class ExecutorService extends orc.orchard.AbstractExecutorService
	implements ExecutorServiceInterface
{
	private URI baseURI;

	public ExecutorService() {
		super(getDefaultLogger());
	}
	
	protected JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration("JAX-WS");
	}
	
	public ExecutorService(URI baseURI, Logger logger) throws RemoteException, MalformedURLException {
		super(logger);
		this.baseURI = baseURI;
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
			ExecutorService executor = new ExecutorService(baseURI);
			executor.logger.info("Binding to '" + baseURI + "'");
			Endpoint.publish(baseURI.toString(), executor);
			executor.logger.info("Bound to '" + baseURI + "'");
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + args[0] + "'");
			return;
		}
	}
}