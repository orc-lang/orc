package orc.orchard;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import orc.ast.oil.Expr;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.java.CompilerService;
import orc.orchard.oil.Oil;


/**
 * Standard implementation of an ExecutorService. Extenders should implement
 * createJobService.
 * 
 * <p>TODO: add executor oil validation to only allow "safe" sites
 * @author quark
 * 
 */
public abstract class AbstractExecutorService implements ExecutorServiceInterface {
	protected Logger logger;
	private AbstractAccounts accounts; 

	protected AbstractExecutorService(Logger logger, AbstractAccounts accounts) {
		this.logger = logger;
		this.accounts = accounts;
	}

	protected AbstractExecutorService(AbstractAccounts accounts) {
		this(getDefaultLogger(), accounts);
	}
	
	/**
	 * Generate a unique unguessable identifier for a job.
	 * @return
	 */
	private String createJobID() {
		// This generates a type 4 random UUID having 124 bits of
		// cryptographically secure randomness, which is unguessable
		// and unique enough for our purposes.
		return UUID.randomUUID().toString();
	}
	
	public String submit(String devKey, Oil program) throws QuotaException, InvalidOilException, RemoteException {
		logger.info("submit");
		try {
			return submitConfigured(devKey, program, getDefaultJobConfiguration());
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
	
	public String submitConfigured(String devKey, Oil program, JobConfiguration configuration)
		throws QuotaException, InvalidOilException,	UnsupportedFeatureException, RemoteException
	{
		if (configuration.debuggable) {
			throw new UnsupportedFeatureException("Debuggable jobs not supported yet.");
		}
		String id = createJobID();
		Expr expr = program.unmarshal();
		OilSecurityValidator validator = new OilSecurityValidator();
		expr.accept(validator);
		if (validator.hasProblems()) {
			StringBuffer sb = new StringBuffer();
			sb.append("OIL security violations:");
			for (OilSecurityValidator.SecurityProblem problem : validator.getProblems()) {
				sb.append("\n");
				sb.append(problem);
			}
			throw new InvalidOilException(sb.toString());
		}
		accounts.getAccount(devKey).addJob(id, new Job(expr, configuration));
		return id;
	}
	
	public Set<String> jobs(String devKey) {
		return accounts.getAccount(devKey).jobIDs();
	}
	
	public String compileAndSubmit(String devKey, String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submit(devKey, compiler.compile(devKey, program));
	}

	public String compileAndSubmitConfigured(String devKey, String program, JobConfiguration configuration) throws QuotaException, InvalidProgramException, InvalidOilException, UnsupportedFeatureException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submitConfigured(devKey, compiler.compile(devKey, program), configuration);
	}
	
	protected static JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration();
	}
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
	
	protected Waiter getWaiter() {
		return new ThreadWaiter();
	}

	public void finishJob(String devKey, String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		accounts.getAccount(devKey).getJob(job).finish();
	}

	public void haltJob(String devKey, String job) throws RemoteException, InvalidJobException {
		accounts.getAccount(devKey).getJob(job).halt();
	}

	public List<JobEvent> jobEvents(String devKey, String job) throws RemoteException, InterruptedException, InvalidJobException {
		return accounts.getAccount(devKey).getJob(job).getEvents(getWaiter());
	}

	public String jobState(String devKey, String job) throws RemoteException, InvalidJobException {
		return accounts.getAccount(devKey).getJob(job).state();
	}

	public void purgeJobEvents(String devKey, String job) throws RemoteException, InvalidJobException {
		accounts.getAccount(devKey).getJob(job).purgeEvents();
	}

	public void startJob(String devKey, String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		accounts.getAccount(devKey).getJob(job).start();
	}

	public void respondToPrompt(String devKey, String job, int promptID, String response) throws InvalidPromptException, RemoteException, InvalidJobException {
		accounts.getAccount(devKey).getJob(job).respondToPrompt(promptID, response);
	}

	public void cancelPrompt(String devKey, String job, int promptID) throws InvalidJobException, InvalidPromptException, RemoteException {
		accounts.getAccount(devKey).getJob(job).cancelPrompt(promptID);
	}
}