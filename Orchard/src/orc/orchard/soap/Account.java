package orc.orchard.soap;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.postgresql.util.PGInterval;

import orc.orchard.Job;
import orc.orchard.errors.QuotaException;

/**
 * Manage a group of jobs associated with a user account. Note that jobs are
 * tracked solely in memory, mainly because it would be a hassle to keep the
 * database up-to-date otherwise.
 * 
 * @author quark
 */
public abstract class Account {	
	private Map<String, Job> jobs = new HashMap<String, Job>();
	private Integer quota = null;
	private Integer eventBufferSize = null;
	private PGInterval lifespan = null;
	
	public Account() {}

	public synchronized void setLifespan(PGInterval lifespan) {
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
		job.onFinish(new Job.FinishListener() {
			public void finished(Job job) {
				removeJob(id);
			}
		});
	}
	
	public synchronized Job getJob(String id) {
		return jobs.get(id);
	}
	
	public synchronized List<Job> jobs() {
		return new LinkedList<Job>(jobs.values());
	}
	
	private synchronized void removeJob(String id) {
		jobs.remove(id);
		if (jobs.size() == 0) onNoMoreJobs();
	}
	
	protected abstract void onNoMoreJobs();
	
	public abstract boolean isGuest();
	
	public synchronized void finishOldJobs() {
		if (lifespan == null) return;
		Date now = new Date();
		for (Job job : jobs.values()) {
			Date death = job.getStartDate();
			lifespan.add(death);
			if (death.before(now)) {
				job.finish();
			}
		}
	}

	public void setEventBufferSize(Integer eventBufferSize) {
		this.eventBufferSize = eventBufferSize;
	} 
}