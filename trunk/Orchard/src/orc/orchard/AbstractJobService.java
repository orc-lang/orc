package orc.orchard;

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