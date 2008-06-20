package orc.orchard;

import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;

/**
 * Job service which multiplexes several jobs.
 * currentJob() should be overridden to choose
 * the job for any given request.
 * @author quark
 */
public abstract class AbstractJobsService implements orc.orchard.api.JobServiceInterface {
	protected abstract Job getCurrentJob() throws RemoteException;

	public JobConfiguration configuration() throws RemoteException {
		return getCurrentJob().configuration();
	}

	public void finish() throws InvalidJobStateException, RemoteException {
		getCurrentJob().finish();
	}
	
	public void halt() throws RemoteException {
		getCurrentJob().halt();
	}

	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException, InterruptedException, RemoteException {
		return getCurrentJob().listen(new ThreadWaiter());
	}

	public List<Publication> publications() throws InvalidJobStateException, RemoteException {
		return getCurrentJob().publications();
	}

	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException, RemoteException {
		return getCurrentJob().publicationsAfter(sequence);
	}

	public void start() throws InvalidJobStateException, RemoteException {
		getCurrentJob().start();
	}

	public String state() throws RemoteException {
		return getCurrentJob().state();
	}
}