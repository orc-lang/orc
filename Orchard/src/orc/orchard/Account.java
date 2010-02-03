//
// Account.java -- Java class Account
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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
import orc.ast.oil.expression.Expression;
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
	private final Map<String, Job> jobs = new HashMap<String, Job>();
	private Integer quota = null;
	private Integer lifespan = null;
	private boolean canSendMail = false;
	private boolean canImportJava = false;
	// NB: right now these limits are hard-coded for all accounts,
	// because otherwise it's too easy to write recursive programs
	// which take down the server.
	private int tokenPoolSize = 1024 * 1024;
	private int stackSize = 1024 * 128;

	public Account() {
	}

	public void setLifespan(final Integer lifespan) {
		this.lifespan = lifespan;
	}

	public void setQuota(final Integer quota) {
		this.quota = quota;
	}

	public boolean getCanSendMail() {
		return canSendMail;
	}

	public void setCanSendMail(final boolean canSendMail) {
		this.canSendMail = canSendMail;
	}

	public boolean getCanImportJava() {
		return canImportJava;
	}

	public void setCanImportJava(final boolean canImportJava) {
		this.canImportJava = canImportJava;
	}

	public synchronized void addJob(final String id, final Expression expr) throws QuotaException, InvalidOilException {
		final Config config = new Config();
		config.setCapability("send mail", canSendMail);
		config.setCapability("import java", canImportJava);
		config.setTokenPoolSize(tokenPoolSize);
		config.setStackSize(stackSize);

		if (!canImportJava) {
			final OilSecurityValidator validator = new OilSecurityValidator();
			expr.accept(validator);
			if (validator.hasProblems()) {
				final StringBuffer sb = new StringBuffer();
				sb.append("OIL security violations:");
				for (final OilSecurityValidator.SecurityProblem problem : validator.getProblems()) {
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
		} catch (final CompilationException e) {
			throw new InvalidOilException(e);
		}
		job.setStartDate(new Date());
		jobs.put(id, job);
		final ObjectName jmxid = JMXUtilities.newObjectName(job, id);
		JMXUtilities.registerMBean(job, jmxid);
		job.onFinish(new Job.FinishListener() {
			public void finished(final Job job) {
				removeJob(id);
				JMXUtilities.unregisterMBean(jmxid);
			}
		});
	}

	public synchronized Job getJob(final String id) throws InvalidJobException {
		final Job out = jobs.get(id);
		if (out == null) {
			throw new InvalidJobException();
		} else {
			return out;
		}
	}

	public synchronized List<Job> jobs() {
		return new LinkedList<Job>(jobs.values());
	}

	public synchronized Set<String> getJobIDs() {
		return new HashSet<String>(jobs.keySet());
	}

	private synchronized void removeJob(final String id) {
		jobs.remove(id);
		if (jobs.size() == 0) {
			onNoMoreJobs();
		}
	}

	protected abstract void onNoMoreJobs();

	public abstract boolean getIsGuest();

	public synchronized void finishOldJobs() {
		if (lifespan == null) {
			return;
		}
		// find old jobs
		final int lifespanmillis = lifespan * 1000;
		final Date now = new Date();
		final LinkedList<Job> old = new LinkedList<Job>();
		for (final Job job : jobs.values()) {
			final Date death = new Date(job.getStartDate().getTime() + lifespanmillis);
			if (death.before(now)) {
				// we can't finish the job now or it will cause
				// a ConcurrentModificationException when it
				// removes itself from the active jobs.
				old.add(job);
			}
		}
		// finish the old jobs we found
		for (final Job job : old) {
			job.finish();
		}
	}

	public synchronized int getNumNewJobs() {
		int out = 0;
		for (final Job job : jobs.values()) {
			if (job.getState().equals("NEW")) {
				out++;
			}
		}
		return out;
	}

	public synchronized int getNumRunningJobs() {
		int out = 0;
		for (final Job job : jobs.values()) {
			if (job.getState().equals("RUNNING")) {
				out++;
			}
		}
		return out;
	}

	public synchronized int getNumBlockedJobs() {
		int out = 0;
		for (final Job job : jobs.values()) {
			if (job.getState().equals("BLOCKED")) {
				out++;
			}
		}
		return out;
	}

	public synchronized int getNumDeadJobs() {
		int out = 0;
		for (final Job job : jobs.values()) {
			if (job.getState().equals("DEAD")) {
				out++;
			}
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

	public void setStackSize(final int stackSize) {
		this.stackSize = stackSize;
	}

	public int getTokenPoolSize() {
		return tokenPoolSize;
	}

	public void setTokenPoolSize(final int tokenPoolSize) {
		this.tokenPoolSize = tokenPoolSize;
	}
}
