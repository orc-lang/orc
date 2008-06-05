package orc.orchard.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.error.InvalidJobStateException;
import orc.orchard.error.UnsupportedFeatureException;

/**
 * Manage an Orc job.
 * @author quark
 */
public interface JobService extends Remote {
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
	 * If the job was started, safely terminate it, with the same semantics as
	 * the termination provided by asymmetric composition. If the job has not
	 * been started yet, mark it as dead. Once a job has been aborted, the
	 * executor is free to garbage collect it, so pointers to aborted jobs may
	 * become invalid at any time.
	 * 
	 * @throws InvalidJobStateException
	 *             if the job was already aborted.
	 */
	public void abort() throws InvalidJobStateException, RemoteException;
	/**
	 * What is the job's state? Possible return values:
	 * NEW: not yet started.
	 * RUNNING: started and processing tokens.
	 * WAITING: started and waiting for response from a site.
	 * DEAD: aborted. 
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
	 * Retrieve all publications made since the last call to listen() or publications().
	 * If no publications were made, block until at least one is made.
	 * @throws InvalidJobStateException if the job is not RUNNING or WAITING.
	 */
	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException, RemoteException;
}