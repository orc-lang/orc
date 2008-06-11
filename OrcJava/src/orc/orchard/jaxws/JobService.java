package orc.orchard.jaxws;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.ast.simple.Expression;
import orc.orchard.InvalidJobStateException;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.UnsupportedFeatureException;

/**
 * You'll notice a bunch of pointlessly-overridden methods annotated with WebMethod.
 * For whatever reason, JAX-WS doesn't work correctly if you don't have these explicit
 * annotations (in clear violation of the spec, by the way).
 * @author quark
 */
@WebService(endpointInterface="orc.orchard.jaxws.JobServiceInterface")
public class JobService extends orc.orchard.AbstractJobService
	implements JobServiceInterface
{
	private Endpoint endpoint;
	
	/** Exists only to satisfy a silly requirement of JAX-WS */
	public JobService() {
		super(null, null, null);
		throw new AssertionError("Do not call this method directly");
	}
	
	public JobService(URI baseURI, Logger logger, JobConfiguration configuration, Expression expression) throws RemoteException, MalformedURLException {
		super(logger, configuration, expression);
		logger.info("Binding to '" + baseURI + "'");
		this.endpoint = Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}

	@Override
	protected void onFinish() throws RemoteException {
		endpoint.stop();
	}
}