package orc.orchard;

import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;

/**
 * Service which delegates to a single Job.
 * @author quark
 */
public abstract class AbstractJobService extends AbstractJobsService {
	protected Job job;
	public AbstractJobService(Job job) {
		this.job = job;
	}
	protected Job getCurrentJob() {
		return job;
	}
}