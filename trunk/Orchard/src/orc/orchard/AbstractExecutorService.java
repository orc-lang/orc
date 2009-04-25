package orc.orchard;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import orc.Config;
import orc.ast.oil.Expr;
import orc.ast.oil.xml.Oil;
import orc.error.compiletime.CompilationException;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.errors.QuotaException;
import orc.orchard.events.JobEvent;
import orc.orchard.java.CompilerService;

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
	private Accounts accounts = Accounts.getAccounts(
			OrchardProperties.getProperty("orc.orchard.Accounts.url"));

	protected AbstractExecutorService(Logger logger) {
		this.logger = logger;
	}

	protected AbstractExecutorService() {
		this(getDefaultLogger());
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
	
	public String submit(String devKey, Oil program)
	throws QuotaException, InvalidOilException,	RemoteException
	{
		logger.info("submit(" + devKey + ", ...)");
		String id = createJobID();
		final Expr expr;
		try {
			expr = program.unmarshal(new Config());
		} catch (CompilationException e) {
			throw new InvalidOilException(e);
		}
		accounts.getAccount(devKey).addJob(id, expr);
		logger.info("submit(" + devKey + ", ...) => " + id);
		return id;
	}
	
	public Set<String> jobs(String devKey) {
		logger.info("jobs(" + devKey + ")");
		return accounts.getAccount(devKey).getJobIDs();
	}
	
	public String compileAndSubmit(String devKey, String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submit(devKey, compiler.compile(devKey, program));
	}
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
	
	protected Waiter getWaiter() {
		return new ThreadWaiter();
	}

	public void finishJob(String devKey, String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		logger.info("finishJob(" + devKey + ", " + job + ")");
		accounts.getAccount(devKey).getJob(job).finish();
	}

	public void haltJob(String devKey, String job) throws RemoteException, InvalidJobException {
		logger.info("haltJob(" + devKey + ", " + job + ")");
		accounts.getAccount(devKey).getJob(job).halt();
	}

	public List<JobEvent> jobEvents(String devKey, String job) throws RemoteException, InterruptedException, InvalidJobException {
		logger.info("jobEvents(" + devKey + ", " + job + ")");
		return accounts.getAccount(devKey).getJob(job).getEvents(getWaiter());
	}

	public String jobState(String devKey, String job) throws RemoteException, InvalidJobException {
		logger.info("jobState(" + devKey + ", " + job + ")");
		return accounts.getAccount(devKey).getJob(job).getState();
	}

	public void purgeJobEvents(String devKey, String job) throws RemoteException, InvalidJobException {
		logger.info("purgeJobEvents(" + devKey + ", " + job + ")");
		accounts.getAccount(devKey).getJob(job).purgeEvents();
	}

	public void startJob(String devKey, String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		logger.info("startJob(" + devKey + ", " + job + ")");
		accounts.getAccount(devKey).getJob(job).start();
	}

	public void respondToPrompt(String devKey, String job, int promptID, String response) throws InvalidPromptException, RemoteException, InvalidJobException {
		logger.info("respondToPrompt(" + devKey + ", " + job + "," + promptID + ", ...)");
		accounts.getAccount(devKey).getJob(job).respondToPrompt(promptID, response);
	}

	public void cancelPrompt(String devKey, String job, int promptID) throws InvalidJobException, InvalidPromptException, RemoteException {
		logger.info("cancelPrompt(" + devKey + ", " + job + "," + promptID + ")");
		accounts.getAccount(devKey).getJob(job).cancelPrompt(promptID);
	}
}