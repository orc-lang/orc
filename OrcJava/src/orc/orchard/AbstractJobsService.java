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
	protected abstract Job currentJob() throws RemoteException;

	public JobConfiguration configuration() throws RemoteException {
		return currentJob().configuration();
	}

	public void finish() throws InvalidJobStateException, RemoteException {
		currentJob().finish();
	}
	
	public void halt() throws RemoteException {
		currentJob().halt();
	}

	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException, InterruptedException, RemoteException {
		return currentJob().listen(new ThreadWaiter());
	}

	public List<Publication> publications() throws InvalidJobStateException, RemoteException {
		return currentJob().publications();
	}

	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException, RemoteException {
		return currentJob().publicationsAfter(sequence);
	}

	public void start() throws InvalidJobStateException, RemoteException {
		currentJob().start();
	}

	public String state() throws RemoteException {
		return currentJob().state();
	}
}