package orc.orchard;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import orc.Config;
import orc.ast.oil.Expr;
import orc.error.compiletime.CompilationException;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.QuotaException;
import orc.orchard.jmx.JMXUtilities;

/**
 * Manage a group of jobs associated with a user account. Note that jobs are
 * tracked solely in memory, mainly because it would be a hassle to keep the
 * database up-to-date otherwise.
 * 
 * @author quark
 */
public abstract class Account implements AccountMBean {	
	private Map<String, Job> jobs = new HashMap<String, Job>();
	private Integer quota = null;
	private Integer lifespan = null;
	private boolean canSendMail = false;
	private boolean canImportJava = false;
	// NB: right now these limits are hard-coded for all accounts,
	// because it's too easy to write recursive programs
	// which take down the server otherwise.
	private int tokenPoolSize = 1024;
	private int stackSize = 1024;

	public Account() {}

	public void setLifespan(Integer lifespan) {
		this.lifespan = lifespan;
	}

	public void setQuota(Integer quota) {
		this.quota = quota;
	}
	
	public boolean getCanSendMail() {
		return canSendMail;
	}

	public void setCanSendMail(boolean canSendMail) {
		this.canSendMail = canSendMail;
	}
	
	public boolean getCanImportJava() {
		return canImportJava;
	}

	public void setCanImportJava(boolean canImportJava) {
		this.canImportJava = canImportJava;
	}

	public synchronized void addJob(final String id, Expr expr) throws QuotaException, InvalidOilException {
		Config config = new Config();
		config.setCapability("send mail", canSendMail);
		config.setCapability("import java", canImportJava);
		config.setTokenPoolSize(tokenPoolSize);
		config.setStackSize(stackSize);
		
		if (!canImportJava) {
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
		}
		
		finishOldJobs();
		if (quota != null && jobs.size() >= quota) {
			throw new QuotaException();
		}
		Job job;
		try {
			job = new Job(expr, config);
		} catch (CompilationException e) {
			throw new InvalidOilException(e);
		}
		job.setStartDate(new Date());
		jobs.put(id, job);
		final ObjectName jmxid = JMXUtilities.newObjectName(job, id);
		JMXUtilities.registerMBean(job, jmxid);
		job.onFinish(new Job.FinishListener() {
			public void finished(Job job) {
				removeJob(id);
				JMXUtilities.unregisterMBean(jmxid);
			}
		});
	}
	
	public synchronized Job getJob(String id) throws InvalidJobException {
		Job out = jobs.get(id);
		if (out == null) throw new InvalidJobException();
		else return out;
	}
	
	public synchronized List<Job> jobs() {
		return new LinkedList<Job>(jobs.values());
	}

	public synchronized Set<String> getJobIDs() {
		return new HashSet<String>(jobs.keySet());
	}
	
	private synchronized void removeJob(String id) {
		jobs.remove(id);
		if (jobs.size() == 0) onNoMoreJobs();
	}
	
	protected abstract void onNoMoreJobs();
	
	public abstract boolean getIsGuest();
	
	public synchronized void finishOldJobs() {
		if (lifespan == null) return;
		// find old jobs
		final int lifespanmillis = lifespan * 1000;
		Date now = new Date();
		LinkedList<Job> old = new LinkedList<Job>();
		for (Job job : jobs.values()) {
			Date death = new Date(job.getStartDate().getTime() + lifespanmillis);
			if (death.before(now)) {
				// we can't finish the job now or it will cause
				// a ConcurrentModificationException when it
				// removes itself from the active jobs.
				old.add(job);
			}
		}
		// finish the old jobs we found
		for (Job job : old) job.finish();
	}

	public synchronized int getNumNewJobs() {
		int out = 0;
		for (Job job : jobs.values()) {
			if (job.getState().equals("NEW")) out++;
		}
		return out;
	}
	
	public synchronized int getNumRunningJobs() {
		int out = 0;
		for (Job job : jobs.values()) {
			if (job.getState().equals("RUNNING")) out++;
		}
		return out;
	}
	
	public synchronized int getNumBlockedJobs() {
		int out = 0;
		for (Job job : jobs.values()) {
			if (job.getState().equals("BLOCKED")) out++;
		}
		return out;
	}
	
	public synchronized int getNumDeadJobs() {
		int out = 0;
		for (Job job : jobs.values()) {
			if (job.getState().equals("DEAD")) out++;
		}
		return out;
	}

	public Integer getLifespan() {
		return lifespan;
	}

	public Integer getQuota() {
		return quota;
	}

	public int getStackSize() {
		return stackSize;
	}

	public void setStackSize(int stackSize) {
		this.stackSize = stackSize;
	}

	public int getTokenPoolSize() {
		return tokenPoolSize;
	}

	public void setTokenPoolSize(int tokenPoolSize) {
		this.tokenPoolSize = tokenPoolSize;
	}
}
