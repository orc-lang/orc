package orc.orchard.jaxws;

import java.rmi.RemoteException;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;

import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.error.InvalidJobStateException;
import orc.orchard.error.UnsupportedFeatureException;
import orc.orchard.interfaces.JobService;

/**
 * The webservice endpointInterface for JobService. Due to some very subtle
 * and annoying issues with the way generics and web service annotations
 * interact (which I'm not convinced aren't bugs), this cannot extend
 * {@link orc.orchard.interfaces.JobService}
 */
@WebService
public interface JobServiceInterface {
	public JobConfiguration configuration() throws RemoteException;
	public void start() throws InvalidJobStateException, RemoteException;
	public void finish() throws InvalidJobStateException, RemoteException;
	public void abort() throws RemoteException;
	public String state() throws RemoteException;
	public List<Publication> publications() throws InvalidJobStateException, RemoteException;
	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException, RemoteException;
	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException, RemoteException;
}