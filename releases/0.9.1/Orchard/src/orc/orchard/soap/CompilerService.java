package orc.orchard.soap;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.orchard.AbstractCompilerService;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.oil.Oil;

/**
 * HACK: We must explicitly declare every published web method in this class, we
 * can't simply inherit them, for the following reasons:
 * <ul>
 * <li>JAX-WS ignores (does not publish) inherited methods. You can work around
 * this by using an endpointInterface which includes all the methods you want to
 * publish, but...
 * <li>JAX-WS JSON bindings don't work with endpointInterface at all.
 * </ul>
 * 
 * @author quark
 */
@WebService
//@BindingType(JSONBindingID.JSON_BINDING)
public class CompilerService extends AbstractCompilerService {
	/**
	 * Construct a service to run in an existing servlet context.
	 */
	public CompilerService() {
		// FIXME: should we be trying to write to the servlet log in some way?
		super(getDefaultLogger());
	}
	
	CompilerService(URI baseURI) {
		this();
		logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}
	
	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("http://localhost:8280/orchard/compiler");
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
		new CompilerService(baseURI);
	}

	/** Do-nothing override */
	@Override
	public Oil compile(@WebParam(name="devKey") String devKey, @WebParam(name="program") String program) throws InvalidProgramException {
		return super.compile(devKey, program);
	}
}