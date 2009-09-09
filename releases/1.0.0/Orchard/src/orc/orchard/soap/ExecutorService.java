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
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.BindingType;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import orc.ast.oil.xml.Oil;
import orc.orchard.AbstractExecutorService;
import orc.orchard.Waiter;
import orc.orchard.errors.InvalidJobException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.errors.QuotaException;
import orc.orchard.events.JobEvent;

import org.jvnet.jax_ws_commons.json.JSONBindingID;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/**
 * HACK: We must explicitly declare every published web method in this class, we
 * can't simply inherit them. See CompilerService for a full explanation.
 * 
 * <p>TODO: develop a JSON binding which translates directly from Java types
 * rather than going through XML.
 * 
 * @author quark
 */
@WebService
@BindingType(JSONBindingID.JSON_BINDING)
public class ExecutorService extends AbstractExecutorService {
	@Resource
	private WebServiceContext context;
	
	/**
	 * This is used in the listen method to take advantage of Jetty's support
	 * for long-running requests (aka AJAX Comet).
	 * <p>
	 * FIXME: It turns out JAX-WS will catch the exception which Jetty uses to
	 * escape from the request on suspend, so this doesn't work. I'm keeping it
	 * here for future reference.
	 * 
	 * @author quark
	 */
	@SuppressWarnings("unused")
	private class JettyContinuationWaiter implements Waiter {
		private Continuation continuation;
		public void resume() {
			continuation.resume();
		}
		public void suspend(Object monitor) throws InterruptedException {
			continuation = ContinuationSupport.getContinuation(getServletRequest(), monitor);
			continuation.suspend(0);
		}
		private HttpServletRequest getServletRequest() {
			MessageContext mc = context.getMessageContext();
			return (HttpServletRequest)mc.get(MessageContext.SERVLET_REQUEST);
		}
	}
	
	static {
		// Register the PostgreSQL driver.
		try {
			DriverManager.registerDriver(new org.postgresql.Driver());
		} catch (SQLException e) {
			// Somehow failed to create the driver?
			// Should be impossible.
			throw new AssertionError(e);
		}
	}
	
	public ExecutorService() {
		super(getDefaultLogger());
	}
	
	/**
	 * If you don't explicitly pass a baseURI, it is assumed you are running in
	 * a servlet container and one will be inferred.
	 * 
	 * @param baseURI
	 */
	ExecutorService(URI baseURI) {
		this();
		logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}
	
	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("http://localhost:8280/orchard/executor");
		} catch (URISyntaxException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
		if (args.length > 0) {
			try {
				baseURI = new URI(args[0]);
			} catch (URISyntaxException e) {
				System.err.println("Invalid URI '" + args[0] + "'");
				return;
			}
		}
		new ExecutorService(baseURI);
	}

	/** Do-nothing override. */
	@Override
	public String compileAndSubmit(@WebParam(name="devKey") String devKey, @WebParam(name="program") String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		return super.compileAndSubmit(devKey, program);
	}

	/** Do-nothing override. */
	@Override
	public String submit(@WebParam(name="devKey") String devKey, @WebParam(name="program") Oil program) throws QuotaException, InvalidOilException, RemoteException {
		return super.submit(devKey, program);
	}

	/** Do-nothing override. 
	 * @throws InvalidJobException */
	@Override
	public void finishJob(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		super.finishJob(devKey, job);
	}

	/** Do-nothing override. 
	 * @throws InvalidJobException */
	@Override
	public void haltJob(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job) throws RemoteException, InvalidJobException {
		super.haltJob(devKey, job);
	}

	/** Do-nothing override. 
	 * @throws InvalidJobException */
	@Override
	public List<JobEvent> jobEvents(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job) throws RemoteException, InterruptedException, InvalidJobException {
		return super.jobEvents(devKey, job);
	}

	/** Do-nothing override. */
	@Override
	public Set<String> jobs(@WebParam(name="devKey") String devKey) {
		return super.jobs(devKey);
	}

	/** Do-nothing override. 
	 * @throws InvalidJobException */
	@Override
	public String jobState(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job) throws RemoteException, InvalidJobException {
		return super.jobState(devKey, job);
	}

	/** Do-nothing override. 
	 * @throws InvalidJobException */
	@Override
	public void purgeJobEvents(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job) throws RemoteException, InvalidJobException {
		super.purgeJobEvents(devKey, job);
	}

	/** Do-nothing override. 
	 * @throws InvalidJobException */
	@Override
	public void startJob(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job) throws InvalidJobStateException, RemoteException, InvalidJobException {
		super.startJob(devKey, job);
	}
	
	/** Do-nothing override. 
	 * @throws InvalidJobException */
	@Override
	public void respondToPrompt(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job, @WebParam(name="promptID") int promptID, @WebParam(name="response") String response) throws InvalidPromptException, RemoteException, InvalidJobException {
		super.respondToPrompt(devKey, job, promptID, response);
	}

	/** Do-nothing override. */
	@Override
	public void cancelPrompt(@WebParam(name="devKey") String devKey, @WebParam(name="job") String job, @WebParam(name="promptID") int promptID) throws InvalidJobException, InvalidPromptException, RemoteException {
		super.cancelPrompt(devKey, job, promptID);
	}
}