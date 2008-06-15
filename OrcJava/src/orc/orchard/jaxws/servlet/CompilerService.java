package orc.orchard.jaxws.servlet;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import orc.orchard.AbstractCompilerService;
import orc.orchard.InvalidProgramException;
import orc.orchard.JobConfiguration;
import orc.orchard.jaxws.CompilerServiceInterface;
import orc.orchard.oil.Oil;

@WebService(endpointInterface="orc.orchard.jaxws.CompilerServiceInterface")
public class CompilerService extends AbstractCompilerService
	implements CompilerServiceInterface
{
	/**
	 * Construct a service to run in an existing servlet context.
	 */
	public CompilerService() {
		// FIXME: should we be trying to write to the servlet log in some way?
		super(getDefaultLogger());
	}
}