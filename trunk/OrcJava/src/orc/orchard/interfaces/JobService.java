package orc.orchard.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.error.InvalidJobStateException;
import orc.orchard.error.UnsupportedFeatureException;

/**
 * Manage an Orc job.
 * 
 * The bounded generics are acting as existential types. This allows
 * implementors to provide concrete implementations of the argument types which
 * are tailored to the needs of their implementation.
 * 
 * Because of the generics, this interface can't be used directly by client
 * code, but it serves as documentation and ensures that implementors are
 * providing the necessary methods.
 * 
 * @author quark
 */
public interface JobService<JC extends JobConfiguration, P extends Publication> extends Remote {
	/**
	 * @return the job's configuration.
	 */
	public JC configuration() throws RemoteException;
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
	 * Equivalent to finish(), but if the job is active, it will be safely
	 * terminated (with the same semantics as the termination provided by
	 * asymmetric composition).
	 */
	public void abort() throws RemoteException;
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
	public List<P> publications() throws InvalidJobStateException, RemoteException;
	/**
	 * Retrieve all publications after the given sequence number.
	 * 
	 * @return List of all publications.
	 * @throws InvalidJobStateException if the job is not RUNNING or WAITING.
	 */
	public List<P> publicationsAfter(int sequence) throws InvalidJobStateException, RemoteException;
	/**
	 * Retrieve all publications made since the last call to listen(). If no
	 * publications were made, block until at least one is made. If you call
	 * this method multiple times concurrently, it is not guaranteed that calls
	 * will return in the order they were made.
	 * 
	 * @throws InvalidJobStateException
	 *             if the job is not RUNNING or WAITING.
	 */
	public List<P> listen() throws InvalidJobStateException, UnsupportedFeatureException, RemoteException;
}