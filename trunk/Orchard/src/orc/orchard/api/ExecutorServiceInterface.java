//
// ExecutorServiceInterface.java -- Java interface ExecutorServiceInterface
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.errors.QuotaException;
import orc.orchard.events.JobEvent;

/**
 * Broker used to create and manage running jobs.
 * 
 * <p>
 * The lifecycle of a job:
 * 
 * <ol>
 * <li>Client calls compile to get a job ID.
 * <li>Client calls jobStart to start the job.
 * <li>In a loop,
 * <ol>
 * <li>Client calls jobEvents to get publications.
 * <li>Client calls purgeJobEvents to clear the publication buffer.
 * </ol>
 * <li>Client may call cancelJob to force the job to end.
 * <li>Job finishes.
 * <li>Client calls finishJob to clean up the job.
 * </ol>
 * 
 * <p>
 * Note that the job publication buffer has a fixed size, so if you don't call
 * purgeJobEvents regularly your job will be suspended when the buffer fills.
 * 
 *  * 
 * <p>
 * Originally the executor only allow clients to create jobs and clients had to
 * use a separate service to manage running jobs. This did make the interfaces
 * cleaner and required fewer arguments, but it created a lot of extra work for
 * both the client and the server, so I combined the services. Some examples of
 * the "extra work" (mostly caused by limitations of the Java web services and
 * servlet environments):
 * <ul>
 * <li>Clients must support dynamically-generated web service proxies. (This is
 * really the killer issue, since some platforms like Java make this hard).
 * <li>It's awkward to pass information between the executor and job services.
 * It requires an out-of-band channel.
 * <li>It requires context to be encoded in the URL, which may not be practical
 * for some services. In RPC-style services it's best if all context is passed
 * explicitly as arguments.
 * <li>Building job URLs requires knowledge about the protocol, so it creates
 * a lot more work to build a new protocol front-end.
 * </ul>
 * 
 * @author quark
 */
public interface ExecutorServiceInterface extends Remote {
	/**
	 * Register a new job for execution.
	 * 
	 * @return String Job ID of new job.
	 * @throws QuotaException
	 *             if registering this job would exceed quotas.
	 * @throws InvalidOilException
	 *             if the program is invalid.
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public String submit(String devKey, String program) throws QuotaException, InvalidOilException, RemoteException;

	/**
	 * Combine compilation and submission into a single step.
	 * This is useful for simple clients that don't want to
	 * bother calling a separate compiler.
	 *
	 * @return String Job ID of new job.
	 * @throws QuotaException
	 *             if registering this job would exceed quotas.
	 * @throws InvalidProgramException in case of compilation error.
	 * @throws InvalidOilException
	 *             if the program is invalid.
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public String compileAndSubmit(String devKey, String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException;

	/**
	 * URIs of unfinished jobs started from this executor.
	 *
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public Set<String> jobs(String devKey) throws RemoteException;

	/**
	 * Begin executing the job.
	 * 
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws InvalidJobStateException
	 *             if the job was already started, or was aborted.
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public void startJob(String devKey, String job) throws InvalidJobException, InvalidJobStateException, RemoteException;

	/**
	 * Indicate that the client is done with the job. The job will be canceled if
	 * necessary.
	 * 
	 * <p>
	 * Once this method is called, the service provider is free to garbage
	 * collect the service and the service URL may become invalid, so no other
	 * methods should be called after this.
	 *
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public void finishJob(String devKey, String job) throws InvalidJobException, RemoteException;

	/**
	 * Cancel the job forcibly.
	 * 
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public void cancelJob(String devKey, String job) throws InvalidJobException, RemoteException;

	/**
	 * What is the job's state? Possible return values:
	 * NEW: not yet started.
	 * RUNNING: started and processing tokens.
	 * BLOCKED: blocked because event buffer is full.
	 * DONE: finished executing. 
	 *
	 * @return the current state of the job.
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public String jobState(String devKey, String job) throws InvalidJobException, RemoteException;

	/**
	 * Retrieve events. If no events occurred, block until at least one occurs.
	 * If the job finishes without any more events happening, an empty list will
	 * be returned.
	 * 
	 * <p>FIXME: ensure clients like web/orc.js can recover from connection timeouts.
	 * 
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws InterruptedException
	 *             if the request times out.
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public List<JobEvent> jobEvents(String devKey, String job) throws InvalidJobException, InterruptedException, RemoteException;

	/**
	 * Purge all events from the event buffer which have been returned by
	 * jobEvents. The client is responsible for calling this method regularly to
	 * keep the event buffer from filling up.
	 *
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public void purgeJobEvents(String devKey, String job) throws InvalidJobException, RemoteException;

	/**
	 * Submit a response to a prompt (initiated by the Prompt site).
	 *
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws InvalidPromptException if the promptID is not valid.
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public void respondToPrompt(String devKey, String job, int promptID, String response) throws InvalidJobException, InvalidPromptException, RemoteException;

	/**
	 * Cancel a prompt (initiated by the Prompt site).
	 *
	 * @throws InvalidJobException if the job ID argument is not in this developer key's jobs  
	 * @throws InvalidPromptException if the promptID is not valid.
	 * @throws RemoteException if remote method invocation fails (Communication failure, marshalling or unmarshalling failure, Protocol error, ...)
	 */
	public void cancelPrompt(String devKey, String job, int promptID) throws InvalidJobException, InvalidPromptException, RemoteException;
}
