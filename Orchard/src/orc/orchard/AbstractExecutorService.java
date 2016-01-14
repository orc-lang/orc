//
// AbstractExecutorService.java -- Java class AbstractExecutorService
// Project Orchard
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
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

import scala.MatchError;
import scala.xml.XML;

import orc.ast.oil.nameless.Expression;
import orc.ast.oil.xml.OrcXML;
import orc.error.loadtime.OilParsingException;
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
 * @author quark
 */
public abstract class AbstractExecutorService implements ExecutorServiceInterface {
    protected static Logger logger = Logger.getLogger("orc.orchard.run");
    public final static Globals<Job, Object> globals = new Globals<Job, Object>();

    protected Accounts getAccounts() {
        return Accounts.getAccounts(OrchardProperties.getProperty("orc.orchard.Accounts.url"));
    }

    protected AbstractExecutorService() {
        super();
    }

    /**
     * Generate a unique unguessable identifier for a job.
     * 
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
        final String id = createJobID();
        logger.finer("Orchard executor: submit(" + devKey + ", ...) => " + id);
        final Expression expr;
        try {
            expr = OrcXML.xmlToAst(XML.loadString(program));
        } catch (final OilParsingException e) {
            throw new InvalidOilException(e);
        } catch (final MatchError e) {
            throw new InvalidOilException(e);
        }
        getAccounts().getAccount(devKey).addJob(id, expr);
        return id;
    }

    @Override
    public Set<String> jobs(final String devKey) {
        logger.finer("Orchard executor: jobs(" + devKey + ")");
        return getAccounts().getAccount(devKey).getJobIDs();
    }

    @Override
    public String compileAndSubmit(final String devKey, final String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
        final CompilerService compiler = new CompilerService();
        return submit(devKey, compiler.compile(devKey, program));
    }

    protected Waiter getWaiter() {
        return new ThreadWaiter();
    }

    @Override
    public void finishJob(final String devKey, final String job) throws RemoteException, InvalidJobException {
        logger.finer("Orchard executor: finishJob(" + devKey + ", " + job + ")");
        getAccounts().getAccount(devKey).getJob(job).finish();
    }

    @Override
    public void cancelJob(final String devKey, final String job) throws RemoteException, InvalidJobException {
        logger.finer("Orchard executor: cancelJob(" + devKey + ", " + job + ")");
        getAccounts().getAccount(devKey).getJob(job).cancel();
    }

    @Override
    public List<JobEvent> jobEvents(final String devKey, final String job) throws RemoteException, InterruptedException, InvalidJobException {
        logger.finer("Orchard executor: jobEvents(" + devKey + ", " + job + ")");
        return getAccounts().getAccount(devKey).getJob(job).getEvents(getWaiter());
    }

    @Override
    public String jobState(final String devKey, final String job) throws RemoteException, InvalidJobException {
        logger.finer("Orchard executor: jobState(" + devKey + ", " + job + ")");
        return getAccounts().getAccount(devKey).getJob(job).getState();
    }

    @Override
    public void purgeJobEvents(final String devKey, final String job) throws RemoteException, InvalidJobException {
        logger.finer("Orchard executor: purgeJobEvents(" + devKey + ", " + job + ")");
        getAccounts().getAccount(devKey).getJob(job).purgeEvents();
    }

    @Override
    public void startJob(final String devKey, final String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
        logger.finer("Orchard executor: startJob(" + devKey + ", " + job + ")");
        getAccounts().getAccount(devKey).getJob(job).start();
    }

    @Override
    public void respondToPrompt(final String devKey, final String job, final int promptID, final String response) throws InvalidPromptException, RemoteException, InvalidJobException {
        logger.finer("Orchard executor: respondToPrompt(" + devKey + ", " + job + "," + promptID + ", ...)");
        getAccounts().getAccount(devKey).getJob(job).respondToPrompt(promptID, response);
    }

    @Override
    public void cancelPrompt(final String devKey, final String job, final int promptID) throws InvalidJobException, InvalidPromptException, RemoteException {
        logger.finer("Orchard executor: cancelPrompt(" + devKey + ", " + job + "," + promptID + ")");
        getAccounts().getAccount(devKey).getJob(job).cancelPrompt(promptID);
    }
}
