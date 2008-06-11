package orc.orchard.java;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.logging.Logger;

import orc.ast.simple.Expression;
import orc.orchard.AbstractJobService;
import orc.orchard.JobConfiguration;
import orc.orchard.Publication;

public class JobService extends AbstractJobService {
	private ExecutorService executor;
	private URI baseURI;
	
	public JobService(ExecutorService executor, URI baseURI, Logger logger, JobConfiguration configuration, Expression expression) {
		super(logger, configuration, expression);
		this.executor = executor;
	}

	@Override
	protected void onFinish() throws RemoteException {
		executor.deleteJob(baseURI);
	}
}
