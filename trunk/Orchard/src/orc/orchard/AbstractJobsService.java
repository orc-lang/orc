package orc.orchard;

import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.errors.InvalidJobStateException;

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

	public List<JobEvent> events() throws InterruptedException, RemoteException {
		return getCurrentJob().events(getWaiter());
	}

	public void purge(int sequence) throws RemoteException {
		getCurrentJob().purgeEvents(sequence);
	}

	public void start() throws InvalidJobStateException, RemoteException {
		getCurrentJob().start();
	}

	public String state() throws RemoteException {
		return getCurrentJob().state();
	}
}