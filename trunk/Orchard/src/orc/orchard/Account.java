package orc.orchard;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.QuotaException;

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
	private Integer eventBufferSize = null;
	private Integer lifespan = null;
	
	public Account() {}

	public synchronized void setLifespan(Integer lifespan) {
		this.lifespan = lifespan;
	}

	public synchronized void setQuota(Integer quota) {
		this.quota = quota;
	}

	public synchronized void addJob(final String id, Job job) throws QuotaException {
		finishOldJobs();
		if (quota != null && jobs.size() >= quota) {
			throw new QuotaException();
		}
		job.setStartDate(new Date());
		job.setEventBufferSize(eventBufferSize);
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
		final int lifespanmillis = lifespan * 1000;
		Date now = new Date();
		for (Job job : jobs.values()) {
			Date death = new Date(job.getStartDate().getTime() + lifespanmillis);
			if (death.before(now)) {
				job.finish();
			}
		}
	}

	public void setEventBufferSize(Integer eventBufferSize) {
		this.eventBufferSize = eventBufferSize;
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

	public synchronized Integer getLifespan() {
		return lifespan;
	}

	public synchronized Integer getQuota() {
		return quota;
	}
}