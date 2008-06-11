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
public interface ExecutorService<O extends Oil, JC extends JobConfiguration> extends Remote {
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
	public URI submitConfigured(O program, JC configuration) throws QuotaException,
			InvalidOilException, UnsupportedFeatureException, RemoteException;
	/**
	 * Register a new job for execution, using a default JobConfiguration.
	 */
	public URI submit(O program) throws QuotaException, InvalidOilException, RemoteException;
}