package orc.orchard.java;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import orc.orchard.AbstractExecutorService;
import orc.orchard.InvalidOilException;
import orc.orchard.JobConfiguration;
import orc.orchard.QuotaException;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.oil.Oil;

public class ExecutorService extends AbstractExecutorService {
	/**
	 * I don't know how to represent local Java objects as URIs, so I'll just
	 * treat them as keys which can be used with lookupJob. That's not so
	 * strange, since to make use of any URI you have to apply some
	 * protocol-specific method to it to get a Java proxy object.
	 */
	Map<URI, JobService> jobs = new HashMap<URI, JobService>();

	@Override
	protected JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration("Java");
	}

	public URI submitConfigured(Oil program, JobConfiguration configuration)
		throws QuotaException, InvalidOilException,
			UnsupportedFeatureException, RemoteException
	{
		try {
			URI out = new URI(jobID());
			jobs.put(out, new JobService(this, out, logger, configuration, program.unmarshal()));
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

	/**
	 * Used by jobs to delete themselves when they are done.
	 * @param jobURI
	 */
	void deleteJob(URI jobURI) {
		jobs.remove(jobURI);
	}
}
