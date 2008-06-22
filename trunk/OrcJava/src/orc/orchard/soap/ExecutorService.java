package orc.orchard.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.BindingType;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.jvnet.jax_ws_commons.json.JSONBindingID;

import com.sun.xml.ws.developer.JAXWSProperties;

import orc.orchard.AbstractExecutorService;
import orc.orchard.AbstractJobService;
import orc.orchard.Job;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.oil.Oil;

/**
 * HACK: We must explicitly declare every published web method in this class, we
 * can't simply inherit them. See CompilerService for a full explanation.
 * 
 * @author quark
 */
@WebService
//@BindingType(JSONBindingID.JSON_BINDING)
public class ExecutorService extends AbstractExecutorService {
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
	ExecutorService(URI baseURI) {
		this();
		this.baseURI = baseURI;
		logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}
	
	// This annotation seems unnecessary but wsgen barfs trying to
	// understand ServletContext if it's not here
	@WebMethod(exclude=true)
	private ServletContext getServletContext() {
		MessageContext mc = context.getMessageContext();
		if (mc == null) return null;
		return (ServletContext)mc.get(MessageContext.SERVLET_CONTEXT);
	}

	@Override
	protected URI createJobService(Job job) throws RemoteException {
		if (baseURI == null) {
			try {
				baseURI = new URI((String)context.getMessageContext().get(
						JAXWSProperties.HTTP_REQUEST_URL));
			} catch (URISyntaxException e) {
				// impossible by construction
				throw new AssertionError(e);
			}
		}
		URI jobURI = baseURI.resolve("jobs/" + job.getID());
		final ServletContext sc = getServletContext();
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
	public URI compileAndSubmit(@WebParam(name="program") String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		return super.compileAndSubmit(program);
	}

	/** Do-nothing override. */
	@Override
	public URI compileAndSubmitConfigured(@WebParam(name="program") String program, @WebParam(name="configuration") JobConfiguration configuration) throws QuotaException, InvalidProgramException, InvalidOilException, UnsupportedFeatureException, RemoteException {
		return super.compileAndSubmitConfigured(program, configuration);
	}

	/** Do-nothing override. */
	@Override
	public Set<URI> jobs() {
		return super.jobs();
	}

	/** Do-nothing override. */
	@Override
	public URI submit(@WebParam(name="program") Oil program) throws QuotaException, InvalidOilException, RemoteException {
		return super.submit(program);
	}

	/** Do-nothing override. */
	@Override
	public URI submitConfigured(@WebParam(name="program") Oil program, @WebParam(name="configuration") JobConfiguration configuration) throws QuotaException, InvalidOilException, UnsupportedFeatureException, RemoteException {
		return super.submitConfigured(program, configuration);
	}
}