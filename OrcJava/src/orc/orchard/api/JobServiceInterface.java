package orc.orchard.api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import javax.jws.WebService;

import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;


/**
 * Manage an Orc job.
 * 
 * @author quark
 */
@WebService
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
	 * Indicate that the client is done with an inactive (not RUNNING or
	 * WAITING) job. Once this method is called, the service provider is free to
	 * garbage collect the service and the service URL may become invalid, so no
	 * other methods should be called after this.
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
	 * Retrieve all publications.
	 * @return List of all publications.
	 * @throws InvalidJobStateException if the job is not RUNNING or WAITING.
	 */
	public List<Publication> publications() throws InvalidJobStateException, RemoteException;
	/**
	 * Retrieve all publications after the given sequence number.
	 * 
	 * @return List of all publications.
	 * @throws InvalidJobStateException if the job is not RUNNING or WAITING.
	 */
	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException, RemoteException;
	/**
	 * Retrieve all publications made since the last call to listen(). If no
	 * publications were made, block until at least one is made. If you call
	 * this method multiple times concurrently, it is not guaranteed that calls
	 * will return in the order they were made.
	 * 
	 * @throws InvalidJobStateException if the job is not RUNNING or WAITING.
	 * @throws InterruptedException if the request times out.
	 */
	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException, RemoteException, InterruptedException;
}