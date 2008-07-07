package orc.orchard.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import orc.orchard.AbstractExecutorService;
import orc.orchard.AbstractJobService;
import orc.orchard.Job;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.api.JobServiceInterface;

public class ExecutorService extends AbstractExecutorService
	implements ExecutorServiceInterface
{
	public class JobService extends AbstractJobService implements JobServiceInterface {
		private URI uri;
		public JobService(URI uri, Job job) throws RemoteException, MalformedURLException {
			super(job);
			this.uri = uri;
			logger.info("Binding to '" + uri + "'");
			UnicastRemoteObject.exportObject(this, 0);
			Naming.rebind(uri.toString(), this);
			logger.info("Bound to '" + uri + "'");
			job.onFinish(new Job.FinishListener() {
				public void finished(Job _) throws RemoteException {
					try {
						Naming.unbind(JobService.this.uri.toString());
					} catch (MalformedURLException e) {
						// impossible by construction
						throw new AssertionError(e);
					} catch (NotBoundException e) {
						// This indicates the user called finish() more than once, which we
						// can safely ignore
					}
				}
			});
		}
	}
	
	private URI baseURI;
	
	public ExecutorService(URI baseURI, Logger logger) throws RemoteException, MalformedURLException {
		super(logger);
		this.baseURI = baseURI;
		logger.info("Binding to '" + baseURI + "'");
		UnicastRemoteObject.exportObject(this, 0);
		Naming.rebind(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}

	public ExecutorService(URI baseURI) throws RemoteException, MalformedURLException {
		this(baseURI, getDefaultLogger());
	}
	
	@Override
	protected URI createJobService(Job job) throws RemoteException {
		try {
			URI out = baseURI.resolve("jobs/" + job.getID());
			new JobService(out, job);
			return out;
		} catch (MalformedURLException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
	}

	public static void main(String[] args) {
		URI baseURI;
		try {
			baseURI = new URI("rmi://localhost/orchard/executor");
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
		try {
			new ExecutorService(baseURI);
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + args[0] + "'");
			return;
		}
	}
}