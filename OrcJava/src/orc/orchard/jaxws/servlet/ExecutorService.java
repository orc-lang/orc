package orc.orchard.jaxws.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.List;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import orc.ast.oil.Expr;
import orc.orchard.InvalidJobStateException;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.jaxws.ExecutorServiceInterface;


@WebService(endpointInterface="orc.orchard.jaxws.ExecutorServiceInterface")
public class ExecutorService extends orc.orchard.AbstractExecutorService
	implements ExecutorServiceInterface
{
	@Resource
	private WebServiceContext context;
	
	/**
	 * Implementation of a job which JobsService will delegate to. 
	 */
	class JobService extends AbstractJobService {
		private String jobID;
		protected JobService(String jobID, URI uri, JobConfiguration configuration, Expr expression) {
			super(uri, configuration, expression);
			this.jobID = jobID;
		}
		@Override
		protected void onFinish() throws RemoteException {
			servletContext().removeAttribute(jobID);
		}
		@Override
		public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException {
			// FIXME: need to use Jetty continuations to avoid keeping
			// a thread open in listen
			return super.listen();
		}
	}
	
	public ExecutorService() {
		super(getDefaultLogger());
	}
	
	private ServletContext servletContext() {
		MessageContext mc = context.getMessageContext();
		return (ServletContext)mc.get(MessageContext.SERVLET_CONTEXT);
	}
	
	private URI jobURI(String jobID) {
		MessageContext mc = context.getMessageContext();
		HttpServletRequest hsr = (HttpServletRequest)mc.get(MessageContext.SERVLET_REQUEST);
		try {
			return new URI(hsr.getRequestURL().toString())
				.resolve("jobs/" + jobID);
		} catch (URISyntaxException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}

	@Override
	protected JobConfiguration getDefaultJobConfiguration() {
		return new JobConfiguration("JAX-WS");
	}

	@Override
	public URI createJob(JobConfiguration configuration, Expr expression) {
		String jobID = jobID();
		URI uri = jobURI(jobID);
		servletContext().setAttribute(jobID, new JobService(jobID, uri, configuration, expression));
		return uri;
	}
}