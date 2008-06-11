package orc.orchard.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.logging.Logger;

import orc.ast.simple.Expression;
import orc.orchard.Publication;
import orc.orchard.JobConfiguration;

public class JobService extends orc.orchard.JobService<JobConfiguration, Publication>
	implements RemoteJobService
{
	private URI baseURI;
	public JobService(URI baseURI, Logger logger, JobConfiguration configuration, Expression expression) throws RemoteException, MalformedURLException {
		super(logger, configuration, expression);
		this.baseURI = baseURI;
		logger.info("Binding to '" + baseURI + "'");
		UnicastRemoteObject.exportObject(this, 0);
		Naming.rebind(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}
	
	public void onFinish() throws RemoteException {
		try {
			Naming.unbind(baseURI.toString());
		} catch (MalformedURLException e) {
			// impossible by construction
			throw new AssertionError(e);
		} catch (NotBoundException e) {
			// This indicates the user called finish() more than once, which we
			// can safely ignore
		}
	}

	@Override
	protected Publication createPublication(int sequence, Date date, Object value) {
		return new Publication(sequence, date, value);
	}
}