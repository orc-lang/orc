package orc.orchard;

import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;

/**
 * Job service which multiplexes several jobs.
 * currentJob should be overridden to choose
 * the job for any given request.
 * getWaiter can be overridden to use a different
 * Waiter for blocking methods.
 * @author quark
 */
public abstract class AbstractJobsService implements orc.orchard.api.JobServiceInterface {
	protected abstract Job getCurrentJob() throws RemoteException;
	protected Waiter getWaiter() {
		return new ThreadWaiter();
	}

	public JobConfiguration configuration() throws RemoteException {
		return getCurrentJob().configuration();
	}

	public void finish() throws InvalidJobStateException, RemoteException {
		getCurrentJob().finish();
	}
	
	public void halt() throws RemoteException {
		getCurrentJob().halt();
	}

	public List<Publication> nextPublications() throws UnsupportedFeatureException, InterruptedException, RemoteException {
		return getCurrentJob().nextPublications(getWaiter());
	}

	public List<Publication> publications() throws RemoteException {
		return getCurrentJob().publications();
	}

	public List<Publication> publicationsAfter(int sequence) throws RemoteException {
		return getCurrentJob().publicationsAfter(sequence);
	}
	
	public List<TokenError> nextErrors() throws UnsupportedFeatureException, InterruptedException, RemoteException {
		return getCurrentJob().nextErrors(getWaiter());
	}

	public List<TokenError> errors() throws RemoteException {
		return getCurrentJob().errors();
	}

	public void start() throws InvalidJobStateException, RemoteException {
		getCurrentJob().start();
	}

	public String state() throws RemoteException {
		return getCurrentJob().state();
	}
}