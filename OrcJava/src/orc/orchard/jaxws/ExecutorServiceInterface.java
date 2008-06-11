package orc.orchard.jaxws;

import java.net.URI;
import java.rmi.RemoteException;

import javax.jws.WebService;

import orc.orchard.JobConfiguration;
import orc.orchard.Oil;
import orc.orchard.error.InvalidOilException;
import orc.orchard.error.QuotaException;
import orc.orchard.error.UnsupportedFeatureException;
import orc.orchard.interfaces.ExecutorService;

/**
 * The webservice endpointInterface for ExecutorService. Due to some very subtle
 * and annoying issues with the way generics and web service annotations
 * interact (which I'm not convinced aren't bugs), this cannot extend
 * {@link orc.orchard.interfaces.ExecutorService}
 */
@WebService
public interface ExecutorServiceInterface {
	public URI submitConfigured(Oil program, JobConfiguration configuration) throws QuotaException,
		InvalidOilException, UnsupportedFeatureException, RemoteException;
	public URI submit(Oil program) throws QuotaException, InvalidOilException, RemoteException;
}