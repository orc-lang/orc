package orc.orchard.soap;

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
import orc.orchard.AbstractJobsService;
import orc.orchard.Job;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.api.JobServiceInterface;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;

/**
 * Multi-plex jobs in a single webservice.
 * This looks up the actual job from the servlet context,
 * based on the URL, and delegates everything to that.
 * @author quark
 */
@WebService(endpointInterface="orc.orchard.api.JobServiceInterface")
public class JobsService extends AbstractJobsService
	// wsgen gives nonsense error methods about this class not implementing
	// the appropriate methods if I don't put this here
	implements JobServiceInterface
{
	@Resource
	private WebServiceContext context;
	private Logger logger;
	
	public JobsService() {
		this.logger = Logger.getLogger(JobsService.class.toString());
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

	@Override
	protected Job currentJob() throws RemoteException {
		String jobID = jobID();
		Object out = servletContext().getAttribute(jobID);
		if (out == null) throw new RemoteException("Job '" + jobID + "' not found.");
		return (Job)out;
	}
}