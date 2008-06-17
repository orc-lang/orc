package orc.orchard.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.orchard.AbstractJobService;
import orc.orchard.Job;
import orc.orchard.api.JobServiceInterface;
import orc.orchard.errors.InvalidJobStateException;

@WebService(endpointInterface="orc.orchard.api.JobServiceInterface")
public class StandaloneJobService extends AbstractJobService
	// wsgen gives nonsense error methods about this class not implementing
	// the appropriate methods if I don't put this here
	implements JobServiceInterface
{
	private Endpoint endpoint;
	/** Added to satisfy stupid JAX-WS requirement. */
	public StandaloneJobService() {
		super(null);
		throw new AssertionError("Never call this constructor.");
	}
	public StandaloneJobService(Logger logger, URI uri, Job job) throws RemoteException, MalformedURLException {
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
}