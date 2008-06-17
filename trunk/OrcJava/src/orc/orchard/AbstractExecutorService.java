package orc.orchard;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import orc.ast.oil.Expr;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.java.CompilerService;
import orc.orchard.oil.Oil;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.result.Result;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;


/**
 * Standard implementation of an ExecutorService. Extenders should implement:
 * <ul>
 * <li>subclass of AbstractJobService
 * <li>constructor
 * <li>getDefaultJobConfiguration
 * <li>createJob
 * </ul>
 * 
 * @author quark
 * 
 */
public abstract class AbstractExecutorService implements ExecutorServiceInterface {
	protected Logger logger;
	protected Executor executor;

	protected AbstractExecutorService(Logger logger) {
		this.logger = logger;
		this.executor = new Executor();
	}

	protected AbstractExecutorService() {
		this(getDefaultLogger());
	}

	protected abstract URI createJobService(Job job) throws RemoteException;
	
	public URI submit(Oil program) throws QuotaException, InvalidOilException, RemoteException {
		logger.info("submit");
		try {
			return submitConfigured(program, getDefaultJobConfiguration());
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
	
	public URI submitConfigured(Oil program, JobConfiguration configuration)
		throws QuotaException, InvalidOilException,	UnsupportedFeatureException, RemoteException
	{
		if (configuration.getDebuggable()) {
			throw new UnsupportedFeatureException("Debuggable jobs not supported yet.");
		}
		Job job = executor.createJob(program, configuration);
		URI out = createJobService(job);
		job.setURI(out);
		return out;
	}
	
	public Set<URI> jobs() {
		HashSet<URI> out = new HashSet<URI>();
		for (Job job : executor.jobs().values())
			out.add(job.getURI());
		return out;
	}
	
	public URI compileAndSubmit(String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		Compiler compiler = new Compiler();
		return submit(compiler.compile(program));
	}

	public URI compileAndSubmitConfigured(String program, JobConfiguration configuration) throws QuotaException, InvalidProgramException, InvalidOilException, UnsupportedFeatureException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submitConfigured(compiler.compile(program), configuration);
	}
	
	protected static JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration();
	}
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}