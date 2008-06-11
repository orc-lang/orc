package orc.orchard;

import java.net.URI;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;


/**
 * Broker used to register jobs for execution.
 * 
 * @author quark
 */
public interface ExecutorServiceInterface {
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
}