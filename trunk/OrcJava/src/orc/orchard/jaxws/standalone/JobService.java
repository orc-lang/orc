package orc.orchard.jaxws.standalone;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.ast.oil.Expr;
import orc.ast.simple.Expression;
import orc.orchard.InvalidJobStateException;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.AbstractExecutorService.AbstractJobService;
import orc.orchard.jaxws.JobServiceInterface;

/**
 * Web services cannot be inner classes, so this delegate
 * provides the web service interface while delegating to
 * the inner class.
 * @author quark
 */
@WebService(endpointInterface="orc.orchard.jaxws.JobServiceInterface")
public class JobService	implements JobServiceInterface
{
	private ExecutorService.JobService delegate;
	
	/** Exists only to satisfy a silly requirement of JAX-WS */
	public JobService() {
		throw new AssertionError("Do not call this method directly");
	}
	
	public JobService(ExecutorService.JobService delegate) {
		this.delegate = delegate;
	}

	public void abort() throws RemoteException {
		delegate.abort();
	}

	public JobConfiguration configuration() {
		return delegate.configuration();
	}

	public void finish() throws InvalidJobStateException, RemoteException {
		delegate.finish();
	}

	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException {
		return delegate.listen();
	}

	public List<Publication> publications() throws InvalidJobStateException {
		return delegate.publications();
	}

	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException {
		return delegate.publicationsAfter(sequence);
	}

	public void start() throws InvalidJobStateException {
		delegate.start();
	}

	public String state() {
		return delegate.state();
	}
}