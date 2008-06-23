package orc.orchard.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

import com.sun.xml.ws.developer.JAXWSProperties;

import orc.ast.oil.Expr;
import orc.ast.simple.Expression;
import orc.orchard.AbstractExecutorService;
import orc.orchard.AbstractJobsService;
import orc.orchard.Job;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.ThreadWaiter;
import orc.orchard.TokenError;
import orc.orchard.Waiter;
import orc.orchard.api.JobServiceInterface;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;

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
	
	@WebMethod(exclude=true)
	private HttpServletRequest getServletRequest() {
		MessageContext mc = context.getMessageContext();
		return (HttpServletRequest)mc.get(MessageContext.SERVLET_REQUEST);
	}
	
	private String getJobID() {
		String path = (String)context.getMessageContext().get(
				JAXWSProperties.HTTP_REQUEST_URL);
		return path.substring(path.lastIndexOf('/')+1);
	}

	@Override
	protected Job getCurrentJob() throws RemoteException {
		String jobID = getJobID();
		Object out = getServletContext().getAttribute(jobID);
		if (out == null) throw new RemoteException("Job '" + jobID + "' not found.");
		return (Job)out;
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
	public List<Publication> publications() throws RemoteException {
		return super.publications();
	}
	
	/** Do-nothing override. */
	@Override
	public List<Publication> nextPublications() throws RemoteException, UnsupportedFeatureException, InterruptedException {
		return super.nextPublications();
	}

	/** Do-nothing override. */
	@Override
	public List<Publication> publicationsAfter(@WebParam(name="sequence") int sequence) throws RemoteException {
		return super.publicationsAfter(sequence);
	}
	
	/** Do-nothing override. */
	@Override
	public List<TokenError> errors() throws RemoteException {
		return super.errors();
	}
	
	/** Do-nothing override. */
	@Override
	public List<TokenError> nextErrors() throws RemoteException, UnsupportedFeatureException, InterruptedException {
		return super.nextErrors();
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