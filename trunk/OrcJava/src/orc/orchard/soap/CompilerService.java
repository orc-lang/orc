package orc.orchard.soap;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import orc.orchard.AbstractCompilerService;
import orc.orchard.api.CompilerServiceInterface;

@WebService(endpointInterface="orc.orchard.api.CompilerServiceInterface")
public class CompilerService extends AbstractCompilerService
	// wsgen gives nonsense error methods about this class not implementing
	// the appropriate methods if I don't put this here
	implements CompilerServiceInterface
{
	/**
	 * Construct a service to run in an existing servlet context.
	 */
	public CompilerService() {
		// FIXME: should we be trying to write to the servlet log in some way?
		super(getDefaultLogger());
	}
	
	public CompilerService(URI baseURI) {
		this();
		logger.info("Binding to '" + baseURI + "'");
		Endpoint.publish(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}
	
	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("http://localhost:8080/orchard/compiler");
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
}