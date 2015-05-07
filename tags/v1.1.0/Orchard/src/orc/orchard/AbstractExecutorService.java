//
// AbstractExecutorService.java -- Java class AbstractExecutorService
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

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import orc.Config;
import orc.ast.oil.expression.Expression;
import orc.ast.xml.Oil;
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

	protected Accounts getAccounts() {
		return Accounts.getAccounts(OrchardProperties.getProperty("orc.orchard.Accounts.url"));
	}

	protected AbstractExecutorService(final Logger logger) {
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

	public String submit(final String devKey, final Oil program) throws QuotaException, InvalidOilException, RemoteException {
		logger.info("submit(" + devKey + ", ...)");
		final String id = createJobID();
		final Expression expr;
		try {
			expr = program.unmarshal(new Config());
		} catch (final CompilationException e) {
			throw new InvalidOilException(e);
		}
		getAccounts().getAccount(devKey).addJob(id, expr);
		logger.info("submit(" + devKey + ", ...) => " + id);
		return id;
	}

	public Set<String> jobs(final String devKey) {
		logger.info("jobs(" + devKey + ")");
		return getAccounts().getAccount(devKey).getJobIDs();
	}

	public String compileAndSubmit(final String devKey, final String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		final CompilerService compiler = new CompilerService(logger);
		return submit(devKey, compiler.compile(devKey, program));
	}

	protected static Logger getDefaultLogger() {
		final Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}

	protected Waiter getWaiter() {
		return new ThreadWaiter();
	}

	public void finishJob(final String devKey, final String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		logger.info("finishJob(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).finish();
	}

	public void haltJob(final String devKey, final String job) throws RemoteException, InvalidJobException {
		logger.info("haltJob(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).halt();
	}

	public List<JobEvent> jobEvents(final String devKey, final String job) throws RemoteException, InterruptedException, InvalidJobException {
		logger.info("jobEvents(" + devKey + ", " + job + ")");
		return getAccounts().getAccount(devKey).getJob(job).getEvents(getWaiter());
	}

	public String jobState(final String devKey, final String job) throws RemoteException, InvalidJobException {
		logger.info("jobState(" + devKey + ", " + job + ")");
		return getAccounts().getAccount(devKey).getJob(job).getState();
	}

	public void purgeJobEvents(final String devKey, final String job) throws RemoteException, InvalidJobException {
		logger.info("purgeJobEvents(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).purgeEvents();
	}

	public void startJob(final String devKey, final String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		logger.info("startJob(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).start();
	}

	public void respondToPrompt(final String devKey, final String job, final int promptID, final String response) throws InvalidPromptException, RemoteException, InvalidJobException {
		logger.info("respondToPrompt(" + devKey + ", " + job + "," + promptID + ", ...)");
		getAccounts().getAccount(devKey).getJob(job).respondToPrompt(promptID, response);
	}

	public void cancelPrompt(final String devKey, final String job, final int promptID) throws InvalidJobException, InvalidPromptException, RemoteException {
		logger.info("cancelPrompt(" + devKey + ", " + job + "," + promptID + ")");
		getAccounts().getAccount(devKey).getJob(job).cancelPrompt(promptID);
	}
}
