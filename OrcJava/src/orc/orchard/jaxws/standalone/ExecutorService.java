package orc.orchard.jaxws.standalone;

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

import orc.ast.oil.Expr;
import orc.orchard.InvalidOilException;
import orc.orchard.JobConfiguration;
import orc.orchard.QuotaException;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.jaxws.ExecutorServiceInterface;
import orc.orchard.jaxws.JobServiceInterface;
import orc.orchard.oil.Oil;

@WebService(endpointInterface="orc.orchard.jaxws.ExecutorServiceInterface")
public class ExecutorService extends orc.orchard.AbstractExecutorService
	implements ExecutorServiceInterface
{
	public class JobService extends AbstractJobService {
		private Endpoint endpoint;
		public JobService(URI uri, JobConfiguration configuration, Expr expression) throws RemoteException, MalformedURLException {
			super(uri, configuration, expression);
			logger.info("Binding to '" + uri + "'");
			this.endpoint = Endpoint.publish(uri.toString(),
					new orc.orchard.jaxws.standalone.JobService(this));
			logger.info("Bound to '" + uri + "'");
		}
		@Override
		protected void onFinish() throws RemoteException {
			endpoint.stop();
		}
	}
	
	private URI baseURI;

	public ExecutorService() {
		super(getDefaultLogger());
	}
	
	@Override
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

	@Override
	public URI createJob(JobConfiguration configuration, Expr expression) throws RemoteException {
		try {
			URI out = this.baseURI.resolve("jobs/" + jobID());
			new JobService(out, configuration, expression);
			return out;
		} catch (MalformedURLException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
	}

	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("http://localhost:8080/orchard/executor");
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