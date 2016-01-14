//
// Account.java -- Java class Account
// Project Orchard
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
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

import orc.OrcOptions;
import orc.ast.oil.nameless.Expression;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.QuotaException;
import orc.orchard.jmx.JMXUtilities;
import orc.script.OrcBindings;

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
    //TODO: right now these limits are hard-coded for all accounts,
    // because otherwise it's too easy to write recursive programs
    // which take down the server.
    private int stackSize = 1024 * 128;
    private int tokenPoolSize = 1024 * 1024;
    private int maxThreads = 6;

    public Account() {
    }

    @Override
    public void setLifespan(final Integer lifespan) {
        this.lifespan = lifespan;
    }

    @Override
    public void setQuota(final Integer quota) {
        this.quota = quota;
    }

    @Override
    public boolean getCanSendMail() {
        return canSendMail;
    }

    @Override
    public void setCanSendMail(final boolean canSendMail) {
        this.canSendMail = canSendMail;
    }

    @Override
    public boolean getCanImportJava() {
        return canImportJava;
    }

    @Override
    public void setCanImportJava(final boolean canImportJava) {
        this.canImportJava = canImportJava;
    }

    public synchronized void addJob(final String id, final Expression expr) throws QuotaException, InvalidOilException {
        final OrcOptions config = new OrcBindings(new java.util.HashMap<String, Object>(OrchardProperties.getMap()));
        config.setRight("send mail", canSendMail);
        config.setRight("import java", canImportJava);
        config.stackSize_$eq(stackSize);
        config.maxTokens_$eq(tokenPoolSize);
        config.maxSiteThreads_$eq(maxThreads);

        if (!canImportJava) {
            final OilSecurityValidator validator = new OilSecurityValidator();
            validator.validate(expr);
            if (validator.hasProblems()) {
                throw new InvalidOilException(validator.getProblems());
            }
        }

        if (quota != null && jobs.size() >= quota) {
            throw new QuotaException();
        }
        final Job job = new Job(id, expr, config, this);
        job.setStartDate(new Date());
        jobs.put(id, job);
        final OrchardTimer.Task jobTimeoutTask = new OrchardTimer.Task() {
            @Override
            public void run() {
                job.finish();
            }
        };
        if (lifespan != null) {
            OrchardTimer.schedule(jobTimeoutTask, lifespan * 1000);
        }
        final ObjectName jmxid = JMXUtilities.newObjectName(job, id);
        JMXUtilities.registerMBean(job, jmxid);
        job.onFinish(new Job.FinishListener() {
            @Override
            public void finished(final Job job) {
                jobTimeoutTask.cancel();
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

    @Override
    public synchronized Set<String> getJobIDs() {
        return new HashSet<String>(jobs.keySet());
    }

    private synchronized void removeJob(final String id) {
        jobs.remove(id);
        if (jobs.size() == 0) {
            onNoMoreJobs();
        }
    }

    public abstract String getUsername();

    protected abstract void onNoMoreJobs();

    @Override
    public abstract boolean getIsGuest();

    @Override
    public synchronized int getNumNewJobs() {
        int out = 0;
        for (final Job job : jobs.values()) {
            if (job.getState().equals("NEW")) {
                out++;
            }
        }
        return out;
    }

    @Override
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

    @Override
    public synchronized int getNumDeadJobs() {
        int out = 0;
        for (final Job job : jobs.values()) {
            if (job.getState().equals("DEAD")) {
                out++;
            }
        }
        return out;
    }

    @Override
    public Integer getLifespan() {
        return lifespan;
    }

    @Override
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

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }
}
