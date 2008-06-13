package orc.orchard;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.logging.Logger;

import orc.orchard.oil.Oil;
import orc.orchard.java.CompilerService;


/**
 * Standard implementation of an ExecutorService. Extenders should provide a
 * constructor and override submit(...) to produce JobService instances.
 * 
 * @author quark
 * 
 */
public abstract class AbstractExecutorService implements orc.orchard.ExecutorServiceInterface {
	protected Logger logger;

	protected AbstractExecutorService(Logger logger) {
		this.logger = logger;
	}

	protected AbstractExecutorService() {
		this(getDefaultLogger());
	}
	
	protected abstract JobConfiguration getDefaultJobConfiguration();
	
	public URI submit(Oil program) throws QuotaException, InvalidOilException, RemoteException {
		logger.info("submit");
		try {
			return submitConfigured(program, getDefaultJobConfiguration());
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
	
	public URI compileAndSubmit(String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submit(compiler.compile(program));
	}

	public URI compileAndSubmitConfigured(String program, JobConfiguration configuration) throws QuotaException, InvalidProgramException, InvalidOilException, UnsupportedFeatureException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submitConfigured(compiler.compile(program), configuration);
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
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}
