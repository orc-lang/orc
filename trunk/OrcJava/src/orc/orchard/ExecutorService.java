package orc.orchard;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.logging.Logger;

import orc.orchard.error.InvalidOilException;
import orc.orchard.error.QuotaException;
import orc.orchard.error.UnsupportedFeatureException;
import orc.orchard.interfaces.Oil;
import orc.orchard.interfaces.JobConfiguration;

/**
 * Standard implementation of an ExecutorService. Extenders should provide a
 * constructor and override submit(...) to produce JobService instances.
 * 
 * @author quark
 * 
 */
public abstract class ExecutorService<O extends Oil, JC extends JobConfiguration>
	implements orc.orchard.interfaces.ExecutorService<O, JC>
{
	protected Logger logger;

	protected ExecutorService(Logger logger) {
		this.logger = logger;
	}

	protected ExecutorService() {
		this(getDefaultLogger());
	}
	
	protected abstract JC getDefaultJobConfiguration();
	
	public URI submit(O program) throws QuotaException, InvalidOilException, RemoteException
	{
		logger.info("submit");
		try {
			return submitConfigured(program, getDefaultJobConfiguration());
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Generate a unique unguessable identifier for a job.
	 * @return
	 */
	protected String jobID() {
		// This generates a type 4 random UUID having 124 bits of
		// cryptographically secure randomness, which is unguessable
		// and unique enough for our purposes.
		return UUID.randomUUID().toString();
	}

	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(ExecutorService.class.toString());
		return out;
	}
}
