package orc.orchard.java;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import orc.ast.oil.Expr;
import orc.orchard.AbstractExecutorService;
import orc.orchard.InvalidOilException;
import orc.orchard.JobConfiguration;
import orc.orchard.QuotaException;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.oil.Oil;

public class ExecutorService extends AbstractExecutorService {
	public class JobService extends AbstractJobService {
		public JobService(URI uri, JobConfiguration configuration, Expr expression) {
			super(uri, configuration, expression);
		}

		@Override
		protected void onFinish() throws RemoteException {
			jobs.remove(getURI());
		}
	}
	
	/**
	 * I don't know how to represent local Java objects as URIs, so I'll just
	 * treat them as keys which can be used with lookupJob. That's not so
	 * strange, since to make use of any URI you have to apply some
	 * protocol-specific method to it to get a Java proxy object.
	 */
	private Map<URI, JobService> jobs = new HashMap<URI, JobService>();

	@Override
	protected JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration("Java");
	}
	
	@Override
	protected URI createJob(JobConfiguration configuration, Expr expression) throws UnsupportedFeatureException, RemoteException {
		try {
			URI out = new URI(jobID());
			jobs.put(out, new JobService(out, configuration, expression));
			return out;
		} catch (URISyntaxException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Find a submitted job.
	 * @param jobURI the URI of the job.
	 * @return the JobService object
	 */
	public JobService lookupJob(URI jobURI) {
		return jobs.get(jobURI);
	}
}
