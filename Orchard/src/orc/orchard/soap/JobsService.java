package orc.orchard.soap;

import java.rmi.RemoteException;
import java.util.List;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import orc.orchard.AbstractJobsService;
import orc.orchard.Job;
import orc.orchard.JobConfiguration;
import orc.orchard.JobEvent;
import orc.orchard.Waiter;
import orc.orchard.errors.InvalidJobStateException;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/**
 * Multi-plex jobs in a single webservice. This looks up the actual job from the
 * servlet context, based on the URL, and delegates everything to that, allowing
 * us to use one physical service to handle several logical jobs.
 * 
 * <p>
 * HACK: We must explicitly declare every published web method in this class, we
 * can't simply inherit them. See CompilerService for a full explanation.
 * 
 * @see StandaloneJobService
 * @author quark
 */
@WebService
public class JobsService extends AbstractJobsService {
	@Resource
	private WebServiceContext context;
	
	public JobsService() {}
	
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
	
	/**
	 * FIXME: override this to use Jetty continuations once JAX-WS supports them
	 */
	@Override
	protected Waiter getWaiter() {
		return super.getWaiter();
	}

	// This annotation seems unnecessary but wsgen barfs trying to
	// understand ServletContext if it's not here
	@WebMethod(exclude=true)
	private ServletContext getServletContext() {
		MessageContext mc = context.getMessageContext();
		return (ServletContext)mc.get(MessageContext.SERVLET_CONTEXT);
	}

	@Override
	protected Job getCurrentJob() throws RemoteException {
		Accounts accounts = (Accounts)getServletContext().getAttribute("orc.orchard.soap.accounts");
		Account account = accounts.getAccount(URLHelper.getDeveloperKey(context));
		String jobID = URLHelper.getJobID(context);
		Job job = account.getJob(jobID);
		if (job == null) throw new RemoteException("Job '" + jobID + "' not found.");
		return job;
	}
	
	/** Do-nothing override. */
	@Override
	public JobConfiguration configuration() throws RemoteException {
		return super.configuration();
	}

	/** Do-nothing override. */
	@Override
	public void finish() throws InvalidJobStateException, RemoteException {
		super.finish();
	}

	/** Do-nothing override. */
	@Override
	public void halt() throws RemoteException {
		super.halt();
	}
	
	/** Do-nothing override. */
	@Override
	public List<JobEvent> events() throws InterruptedException, RemoteException {
		return super.events();
	}

	/** Do-nothing override. */
	@Override
	public void purge(@WebParam(name="sequence") int sequence) throws RemoteException {
		super.purge(sequence);
	}

	/** Do-nothing override. */
	@Override
	public void start() throws InvalidJobStateException, RemoteException {
		super.start();
	}

	/** Do-nothing override. */
	@Override
	public String state() throws RemoteException {
		return super.state();
	}
}