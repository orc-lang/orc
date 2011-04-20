//
// AbstractExecutorService.java -- Java class AbstractExecutorService
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
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

import orc.ast.oil.nameless.Expression;
import orc.ast.oil.xml.OrcXML;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.errors.QuotaException;
import orc.orchard.events.JobEvent;
import orc.orchard.java.CompilerService;
import scala.MatchError;
import scala.xml.XML;

/**
 * Standard implementation of an ExecutorService. Extenders should implement
 * createJobService.
 *
 * @author quark
 * 
 */
public abstract class AbstractExecutorService implements ExecutorServiceInterface {
	protected Logger logger;
	public final static Globals<Job, Object> globals = new Globals<Job, Object>();

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

	@Override
	public String submit(final String devKey, final String program) throws QuotaException, InvalidOilException, RemoteException {
		logger.info("submit(" + devKey + ", ...)");
		final String id = createJobID();
		final Expression expr;
		try {
			expr = OrcXML.xmlToAst(XML.loadString(program));
		} catch (final MatchError e) {//FIXME:Any other exceptions?
			throw new InvalidOilException(e);
		}
		getAccounts().getAccount(devKey).addJob(id, expr);
		logger.info("submit(" + devKey + ", ...) => " + id);
		return id;
	}

	@Override
	public Set<String> jobs(final String devKey) {
		logger.info("jobs(" + devKey + ")");
		return getAccounts().getAccount(devKey).getJobIDs();
	}

	@Override
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

	@Override
	public void finishJob(final String devKey, final String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		logger.info("finishJob(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).finish();
	}

	@Override
	public void haltJob(final String devKey, final String job) throws RemoteException, InvalidJobException {
		logger.info("haltJob(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).halt();
	}

	@Override
	public List<JobEvent> jobEvents(final String devKey, final String job) throws RemoteException, InterruptedException, InvalidJobException {
		logger.info("jobEvents(" + devKey + ", " + job + ")");
		return getAccounts().getAccount(devKey).getJob(job).getEvents(getWaiter());
	}

	@Override
	public String jobState(final String devKey, final String job) throws RemoteException, InvalidJobException {
		logger.info("jobState(" + devKey + ", " + job + ")");
		return getAccounts().getAccount(devKey).getJob(job).getState();
	}

	@Override
	public void purgeJobEvents(final String devKey, final String job) throws RemoteException, InvalidJobException {
		logger.info("purgeJobEvents(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).purgeEvents();
	}

	@Override
	public void startJob(final String devKey, final String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		logger.info("startJob(" + devKey + ", " + job + ")");
		getAccounts().getAccount(devKey).getJob(job).start();
	}

	@Override
	public void respondToPrompt(final String devKey, final String job, final int promptID, final String response) throws InvalidPromptException, RemoteException, InvalidJobException {
		logger.info("respondToPrompt(" + devKey + ", " + job + "," + promptID + ", ...)");
		getAccounts().getAccount(devKey).getJob(job).respondToPrompt(promptID, response);
	}

	@Override
	public void cancelPrompt(final String devKey, final String job, final int promptID) throws InvalidJobException, InvalidPromptException, RemoteException {
		logger.info("cancelPrompt(" + devKey + ", " + job + "," + promptID + ")");
		getAccounts().getAccount(devKey).getJob(job).cancelPrompt(promptID);
	}
}
