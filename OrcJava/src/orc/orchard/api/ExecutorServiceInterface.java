package orc.orchard.api;

import java.net.URI;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import javax.jws.WebService;

import orc.orchard.JobConfiguration;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.oil.Oil;


/**
 * Broker used to register jobs for execution.
 * 
 * @author quark
 */
public interface ExecutorServiceInterface extends Remote {
	/**
	 * Register a new job for execution, using the provided job configuration.
	 * 
	 * @return URI locating an instance of JobService.
	 * @throws QuotaException
	 *             if registering this job would exceed quotas.
	 * @throws InvalidOilException
	 *             if the program is invalid.
	 * @throws UnsupportedFeatureException
	 *             if the executor does not support some part of the
	 *             configuration.
	 */
	public URI submitConfigured(Oil program, JobConfiguration configuration) throws QuotaException,
			InvalidOilException, UnsupportedFeatureException, RemoteException;
	/**
	 * Register a new job for execution, using a default JobConfiguration.
	 */
	public URI submit(Oil program) throws QuotaException, InvalidOilException, RemoteException;
	/**
	 * Combine compilation and submission into a single step.
	 * This is useful for simple clients that don't want to
	 * bother calling a separate compiler.
	 */
	public URI compileAndSubmit(String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException;
	/**
	 * Combine compilation and submission into a single step.
	 */
	public URI compileAndSubmitConfigured(String program, JobConfiguration configuration) throws QuotaException, InvalidProgramException, InvalidOilException, UnsupportedFeatureException, RemoteException;
	/**
	 * URIs of unfinished jobs started from this executor.
	 */
	public Set<URI> jobs() throws RemoteException;
}