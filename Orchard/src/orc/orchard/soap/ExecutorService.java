//
// ExecutorService.java -- Java class ExecutorService
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.soap;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;

import orc.orchard.AbstractExecutorService;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.errors.QuotaException;
import orc.orchard.events.JobEvent;

import org.jvnet.jax_ws_commons.json.JSONBindingID;

/**
 * Web Service implementation bean for the Orc executor service.
 * <p>
 * HACK: We must explicitly declare every published web method in this class, we
 * can't simply inherit them. See CompilerService for a full explanation.
 * <p>
 * TODO: develop a JSON binding which translates directly from Java types rather
 * than going through XML.
 *
 * @author quark
 */
@WebService
@BindingType(JSONBindingID.JSON_BINDING)
public class ExecutorService extends AbstractExecutorService {
    @Resource
    private WebServiceContext context;

    static {
        // Register the PostgreSQL driver.
        try {
            DriverManager.registerDriver(new org.postgresql.Driver());
        } catch (final SQLException e) {
            // Somehow failed to create the driver?
            // Should be impossible.
            throw new AssertionError(e);
        }
    }

    public ExecutorService() {
        super();
    }

    /**
     * If you don't explicitly pass a baseURI, it is assumed you are running in
     * a servlet container and one will be inferred.
     *
     * @param baseURI
     */
    ExecutorService(final URI baseURI) {
        this();
        logger.fine("Orchard executor Web service: Publishing endpoint at '" + baseURI + "'");
        Endpoint.publish(baseURI.toString(), this);
        logger.config("Orchard executor Web service: Published endpoint at '" + baseURI + "'");
    }

    @SuppressWarnings("unused")
    public static void main(final String[] args) {
        URI baseURI = URI.create("http://localhost:8280/orchard/executor");
        if (args.length > 0) {
            try {
                baseURI = new URI(args[0]);
            } catch (final URISyntaxException e) {
                System.err.println("Invalid URI '" + args[0] + "'");
                return;
            }
        }
        new ExecutorService(baseURI);
    }

    /** Do-nothing override. */
    @Override
    public String compileAndSubmit(@WebParam(name = "devKey") final String devKey, @WebParam(name = "program") final String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
        return super.compileAndSubmit(devKey, program);
    }

    /** Do-nothing override. */
    @Override
    public String submit(@WebParam(name = "devKey") final String devKey, @WebParam(name = "program") final String program) throws QuotaException, InvalidOilException, RemoteException {
        return super.submit(devKey, program);
    }

    /**
     * Do-nothing override.
     * 
     * @throws InvalidJobException
     */
    @Override
    public void finishJob(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job) throws RemoteException, InvalidJobException {
        super.finishJob(devKey, job);
    }

    /**
     * Do-nothing override.
     * 
     * @throws InvalidJobException
     */
    @Override
    public void cancelJob(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job) throws RemoteException, InvalidJobException {
        super.cancelJob(devKey, job);
    }

    /**
     * Do-nothing override.
     * 
     * @throws InvalidJobException
     */
    @Override
    public List<JobEvent> jobEvents(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job) throws RemoteException, InterruptedException, InvalidJobException {
        return super.jobEvents(devKey, job);
    }

    /** Do-nothing override. */
    @Override
    public Set<String> jobs(@WebParam(name = "devKey") final String devKey) {
        return super.jobs(devKey);
    }

    /**
     * Do-nothing override.
     * 
     * @throws InvalidJobException
     */
    @Override
    public String jobState(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job) throws RemoteException, InvalidJobException {
        return super.jobState(devKey, job);
    }

    /**
     * Do-nothing override.
     * 
     * @throws InvalidJobException
     */
    @Override
    public void purgeJobEvents(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job) throws RemoteException, InvalidJobException {
        super.purgeJobEvents(devKey, job);
    }

    /**
     * Do-nothing override.
     * 
     * @throws InvalidJobException
     */
    @Override
    public void startJob(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
        super.startJob(devKey, job);
    }

    /**
     * Do-nothing override.
     * 
     * @throws InvalidJobException
     */
    @Override
    public void respondToPrompt(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job, @WebParam(name = "promptID") final int promptID, @WebParam(name = "response") final String response) throws InvalidPromptException, RemoteException, InvalidJobException {
        super.respondToPrompt(devKey, job, promptID, response);
    }

    /** Do-nothing override. */
    @Override
    public void cancelPrompt(@WebParam(name = "devKey") final String devKey, @WebParam(name = "job") final String job, @WebParam(name = "promptID") final int promptID) throws InvalidJobException, InvalidPromptException, RemoteException {
        super.cancelPrompt(devKey, job, promptID);
    }
}
