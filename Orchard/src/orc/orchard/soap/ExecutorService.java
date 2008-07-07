package orc.orchard.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;

import orc.orchard.AbstractExecutorService;
import orc.orchard.Job;
import orc.orchard.JobConfiguration;
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
	private Accounts accounts;
	/** Cached URI of the service. */
	private URI baseURI;
	
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
		accounts = new Accounts("jdbc:postgresql://localhost/orchard?user=orchard&password=ckyogack"); 
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
	
	private Account getAccount() {
		return accounts.getAccount(URLHelper.getDeveloperKey(context));
	}

	@Override
	protected URI createJobService(Job job) throws QuotaException, RemoteException {
		ServletContext sc = URLHelper.getServletContext(context);
		getAccount().addJob(job.getID(), job);
		if (sc == null) {
			// We're not running inside a servlet -- launch the standalone
			// job service
			try {
				URI jobURI = baseURI.resolve("jobs/" + job.getID());
				new StandaloneJobService(logger, jobURI, job);
				return jobURI;
			} catch (MalformedURLException e) {
				// impossible by construction
				throw new AssertionError(e);
			}
		} else {
			// it is of course unnecessary to set the accounts object on every
			// request, but the JAX-WS API doesn't provide any way to do
			// post-init setup using the servlet context
			sc.setAttribute("orc.orchard.soap.accounts", accounts);
			try {
				return URLHelper.getJobServiceURL(context, job.getID()).toURI();
			} catch (URISyntaxException e1) {
				// impossible by construction
				throw new AssertionError(e1);
			}
		}
	}
	
	/**
	 * Overridden to only return the jobs relevant to the developer account.
	 */
	public Set<URI> jobs() {
		HashSet<URI> out = new HashSet<URI>();
		for (Job job : getAccount().jobs())
			out.add(job.getURI());
		return out;
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
	public URI submit(@WebParam(name="program") Oil program) throws QuotaException, InvalidOilException, RemoteException {
		return super.submit(program);
	}

	/** Do-nothing override. */
	@Override
	public URI submitConfigured(@WebParam(name="program") Oil program, @WebParam(name="configuration") JobConfiguration configuration) throws QuotaException, InvalidOilException, UnsupportedFeatureException, RemoteException {
		return super.submitConfigured(program, configuration);
	}
}