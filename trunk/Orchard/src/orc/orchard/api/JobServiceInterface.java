package orc.orchard.api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.JobConfiguration;
import orc.orchard.JobEvent;
import orc.orchard.errors.InvalidJobStateException;


/**
 * Manage an Orc job. The lifecycle of a job looks like this:
 * <ol>
 * <li>Client calls start() to start the job.
 * <li>In a loop,
 *     <ol>
 *     <li>Client calls events() to get publications.
 *     <li>Client calls purge() to clear the publication buffer.
 *     </ol>
 * <li>Client may call halt() to force the job to end.
 * <li>Job finishes.
 * <li>Client calls finish() to clean up the job.
 * <li>
 * </ol>
 * 
 * <p>Note that the job publication buffer has a fixed size, so if you don't
 * call purgeEvents regularly your job may be suspended.
 * 
 * @author quark
 */
public interface JobServiceInterface extends Remote {
	/**
	 * @return the job's configuration.
	 */
	public JobConfiguration configuration() throws RemoteException;
	/**
	 * Begin executing the job.
	 * 
	 * @throws InvalidJobStateException
	 *             if the job was already started, or was aborted.
	 */
	public void start() throws InvalidJobStateException, RemoteException;
	/**
	 * Indicate that the client is done with the job. The job will be halted if
	 * necessary.
	 * 
	 * <p>
	 * Once this method is called, the service provider is free to garbage
	 * collect the service and the service URL may become invalid, so no other
	 * methods should be called after this.
	 * 
	 * @throws InvalidJobStateException
	 *             if the job is RUNNING or WAITING.
	 * @throws RemoteException
	 */
	public void finish() throws InvalidJobStateException, RemoteException;
	/**
	 * Halt the job safely, using the same termination semantics as the "pull"
	 * combinator.
	 */
	public void halt() throws RemoteException;
	/**
	 * What is the job's state? Possible return values:
	 * NEW: not yet started.
	 * RUNNING: started and processing tokens.
	 * WAITING: started and waiting for response from a site.
	 * DONE: finished executing. 
	 * @return the current state of the job.
	 */
	public String state() throws RemoteException;
	/**
	 * Retrieve events. If no events occurred, block until at least one occurs.
	 * If the job finishes without any more events happening, an empty list will
	 * be returned.
	 * 
	 * @throws InterruptedException
	 *             if the request times out.
	 */
	public List<JobEvent> events() throws RemoteException, InterruptedException;
	/**
	 * Purge all events from the event buffer with sequence number less than or
	 * equal to the argument. The client is responsible for calling this method
	 * regularly to keep the event buffer from filling up.
	 * 
	 * @throws RemoteException
	 */
	public void purge(int sequence) throws RemoteException;
}