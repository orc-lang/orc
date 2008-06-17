package orc.orchard;

import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;

/**
 * Service which delegates to a single Job.
 * @author quark
 */
public abstract class AbstractJobService implements orc.orchard.api.JobServiceInterface {
	protected Job job;
	public AbstractJobService(Job job) {
		this.job = job;
	}

	public JobConfiguration configuration() throws RemoteException {
		return job.configuration();
	}

	public void finish() throws InvalidJobStateException, RemoteException {
		job.finish();
	}
	
	public void halt() throws RemoteException {
		job.halt();
	}

	/**
	 * You may want to override this to use a different Waiter.
	 */
	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException, InterruptedException, RemoteException {
		return job.listen(new ThreadWaiter());
	}

	public List<Publication> publications() throws InvalidJobStateException, RemoteException {
		return job.publications();
	}

	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException, RemoteException {
		return job.publicationsAfter(sequence);
	}

	public void start() throws InvalidJobStateException, RemoteException {
		job.start();
	}

	public String state() throws RemoteException {
		return job.state();
	}
}