package orc.orchard.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.List;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import orc.orchard.AbstractJobService;
import orc.orchard.Job;
import orc.orchard.Publication;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;


@WebService(endpointInterface="orc.orchard.api.ExecutorServiceInterface")
public class ExecutorService extends orc.orchard.AbstractExecutorService
	// wsgen gives nonsense error methods about this class not implementing
	// the appropriate methods if I don't put this here
	implements ExecutorServiceInterface
{
	@Resource
	private WebServiceContext context;
	/** Cached URI of the service. */
	private URI baseURI;
	
	public ExecutorService() {
		super(getDefaultLogger());
	}
	
	/**
	 * If you don't explicitly pass a baseURI, it is assumed you are running in
	 * a servlet container and one will be inferred.
	 * 
	 * @param baseURI
	 */
	public ExecutorService(URI baseURI) {
		this();
		this.baseURI = baseURI;
		logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}
	
	private ServletContext servletContext() {
		MessageContext mc = context.getMessageContext();
		if (mc == null) return null;
		return (ServletContext)mc.get(MessageContext.SERVLET_CONTEXT);
	}

	@Override
	public URI createJobService(Job job) throws RemoteException {
		if (baseURI == null) {
			MessageContext mc = context.getMessageContext();
			HttpServletRequest hsr = (HttpServletRequest)mc.get(MessageContext.SERVLET_REQUEST);
			try {
				baseURI = new URI(hsr.getRequestURL().toString());
			} catch (URISyntaxException e) {
				// impossible by construction
				throw new AssertionError(e);
			}
		}
		URI jobURI = baseURI.resolve("jobs/" + job.getID());
		final ServletContext sc = servletContext();
		if (sc != null) {
			sc.setAttribute(job.getID(), job);
			job.onFinish(new Job.FinishListener() {
				public void finished(Job job) {
					sc.removeAttribute(job.getID());
				}
			});
		} else {
			try {
				new StandaloneJobService(logger, jobURI, job);
			} catch (MalformedURLException e) {
				// impossible by construction
				throw new AssertionError(e);
			}
		}
		return jobURI;
	}
	
	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("http://localhost:8080/orchard/executor");
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
}