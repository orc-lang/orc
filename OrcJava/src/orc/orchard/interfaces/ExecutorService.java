package orc.orchard.interfaces;

import java.net.URI;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import orc.orchard.error.InvalidOilException;
import orc.orchard.error.QuotaException;
import orc.orchard.error.UnsupportedFeatureException;

/**
 * Broker used to register jobs for execution. 
 * @author quark
 */
public interface ExecutorService extends Remote {
	/**
	 * Register a new job for execution, using a default job configuration.
	 *  
	 * @param program Orc program to run. 
	 * @return URI locating an instance of JobService.
	 * @throws QuotaException if registering this job would exceed quotas.
	 * @throws InvalidOilException if the program is invalid.
	 */
	public URI submit(Oil program) throws QuotaException, InvalidOilException, RemoteException;
	/**
	 * Register a new job for execution, using the provided job configuration.
	 * 
	 * @throws UnsupportedFeatureException
	 *             if the executor does not support some part of the
	 *             configuration.
	 */
	public URI submit(Oil program, JobConfiguration configuration) throws QuotaException, InvalidOilException, UnsupportedFeatureException, RemoteException;
	/**
	 * List registered job URIs.
	 * 
	 * @throws UnsupportedFeatureException if the executor does not support this method.
	 */
	public Set<URI> jobs() throws UnsupportedFeatureException, RemoteException;
}