package orc.orchard.jaxws.servlet;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import orc.ast.oil.Expr;
import orc.ast.simple.Expression;
import orc.orchard.AbstractExecutorService;
import orc.orchard.InvalidJobStateException;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.UnsupportedFeatureException;
import orc.orchard.jaxws.JobServiceInterface;

/**
 * Multi-plex jobs in a single webservice.
 * This looks up the actual job from the servlet context,
 * based on the URL, and delegates everything to that.
 * @author quark
 */
@WebService(endpointInterface="orc.orchard.jaxws.JobServiceInterface")
public class JobsService implements JobServiceInterface {
	@Resource
	private WebServiceContext context;
	private Logger logger;
	
	public JobsService() {
		this.logger = Logger.getLogger(JobsService.class.toString());
	}

	private ExecutorService.JobService delegate() throws RemoteException {
		String jobID = jobID();
		Object out = servletContext().getAttribute(jobID);
		if (out == null) throw new RemoteException("Job '" + jobID + "' not found.");
		return (ExecutorService.JobService)out;
	}
	
	private ServletContext servletContext() {
		MessageContext mc = context.getMessageContext();
		return (ServletContext)mc.get(MessageContext.SERVLET_CONTEXT);
	}
	
	private String jobID() {
		MessageContext mc = context.getMessageContext();
		HttpServletRequest sr = (HttpServletRequest)mc.get(MessageContext.SERVLET_REQUEST);
		String path = sr.getServletPath();
		logger.info(path);
		return path.substring(path.lastIndexOf('/')+1);
	}
	
	public void abort() throws RemoteException {
		delegate().abort();
	}

	public JobConfiguration configuration() throws RemoteException {
		return delegate().configuration();
	}

	public void finish() throws InvalidJobStateException, RemoteException {
		delegate().finish();
	}

	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException, RemoteException {
		return delegate().listen();
	}

	public List<Publication> publications() throws InvalidJobStateException, RemoteException {
		return delegate().publications();
	}

	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException, RemoteException {
		return delegate().publicationsAfter(sequence);
	}

	public void start() throws InvalidJobStateException, RemoteException {
		delegate().start();
	}

	public String state() throws RemoteException {
		return delegate().state();
	}
}