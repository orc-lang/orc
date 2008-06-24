package orc.orchard.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Logger;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.Endpoint;

import org.jvnet.jax_ws_commons.json.JSONBindingID;

import orc.orchard.AbstractJobService;
import orc.orchard.Job;
import orc.orchard.JobConfiguration;
import orc.orchard.JobEvent;
import orc.orchard.TokenErrorEvent;
import orc.orchard.api.JobServiceInterface;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;

/**
 * A service for a single job. This is used with the built-in Endpoint
 * HTTP service, which makes it easy to create new service URLs on
 * demand, but won't work in a servlet context, where we need to have
 * one service to handle all jobs.
 * 
 * <p>HACK: We must explicitly declare every published web method in this
 * class, we can't simply inherit them. See CompilerService for a full
 * explanation.
 * 
 * @see JobsService
 * @author quark
 */
@WebService
//@BindingType(JSONBindingID.JSON_BINDING)
public class StandaloneJobService extends AbstractJobService {
	private Endpoint endpoint;
	/** Added to satisfy stupid JAX-WS requirement. */
	public StandaloneJobService() {
		super(null);
		throw new AssertionError("Never call this constructor.");
	}
	
	StandaloneJobService(Logger logger, URI uri, Job job) throws RemoteException, MalformedURLException {
		super(job);
		logger.info("Binding to '" + uri + "'");
		this.endpoint = Endpoint.publish(uri.toString(), this);
		logger.info("Bound to '" + uri + "'");
		job.onFinish(new Job.FinishListener() {
			public void finished(Job _) {
				StandaloneJobService.this.endpoint.stop();
			}
		});
	}
	
	/** Do-nothing override. */
	@Override
	public JobConfiguration configuration() throws RemoteException {
		return super.configuration();
	}

	/** Do-nothing override. */
	@Override
	public void finish() throws InvalidJobStateException, RemoteException {
		super.finish();
	}

	/** Do-nothing override. */
	@Override
	public void halt() throws RemoteException {
		super.halt();
	}

	/** Do-nothing override. */
	@Override
	public List<JobEvent> listen() throws UnsupportedFeatureException, InterruptedException, RemoteException {
		return super.listen();
	}

	/** Do-nothing override. */
	@Override
	public List<JobEvent> events() throws RemoteException {
		return super.events();
	}

	/** Do-nothing override. */
	@Override
	public List<JobEvent> eventsAfter(@WebParam(name="sequence") int sequence) throws RemoteException {
		return super.eventsAfter(sequence);
	}

	/** Do-nothing override. */
	@Override
	public void start() throws InvalidJobStateException, RemoteException {
		super.start();
	}

	/** Do-nothing override. */
	@Override
	public String state() throws RemoteException {
		return super.state();
	}
}