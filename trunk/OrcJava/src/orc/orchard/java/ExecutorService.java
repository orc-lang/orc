package orc.orchard.java;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import orc.ast.oil.Expr;
import orc.orchard.AbstractExecutorService;
import orc.orchard.AbstractJobService;
import orc.orchard.Job;
import orc.orchard.JobConfiguration;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.oil.Oil;

/**
 * I don't know how to represent local Java objects as URIs, so I'll just treat
 * them as keys which can be used with lookupJob. That's not so strange, since
 * to make use of any URI you have to apply some protocol-specific method to it
 * to get a Java proxy object.
 */
public class ExecutorService extends AbstractExecutorService {
	public class JobService extends AbstractJobService {
		public JobService(Job job) {
			super(job);
		}
	}

	@Override
	protected URI createJobService(Job job) {
		try {
			return new URI(job.getID());
		} catch (URISyntaxException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}

	public final class JobNotFoundException extends Exception {
		public JobNotFoundException() {
			super();
		}
	}
	
	/**
	 * Find a submitted job.
	 * @param jobURI the URI of the job.
	 * @return the JobService object
	 */
	public JobService lookupJob(URI jobURI) throws JobNotFoundException {
		Job job = executor.jobs().get(jobURI.toString());
		if (job == null) throw new JobNotFoundException();
		return new JobService(job);
	}
}