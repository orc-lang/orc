package orc.orchard.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import orc.ast.simple.Expression;
import orc.orchard.error.InvalidJobStateException;
import orc.orchard.error.UnsupportedFeatureException;
import orc.orchard.interfaces.JobConfiguration;
import orc.orchard.interfaces.Publication;
import orc.runtime.Environment;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.result.QueueResult;
import orc.runtime.nodes.result.Result;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

public class JobService extends UnicastRemoteObject implements orc.orchard.interfaces.JobService {
	private boolean isNew = true;
	private JobConfiguration configuration;
	private Node node;
	private OrcEngine engine = new OrcEngine();
	/** Hold publications temporarily until they are needed by the client. */
	private BlockingQueue<Publication> publicationBuffer = new LinkedBlockingQueue<Publication>();
	/** History of all published values. */
	private List<Publication> publications = new LinkedList<Publication>();
	private Logger logger;
	
	public JobService(JobConfiguration configuration, Expression expression, Logger logger_) throws RemoteException {
		super();
		this.configuration = configuration;
		this.logger = logger_;
		this.node = expression.compile(new Result() {
			int sequence = 1;
			public synchronized void emit(Value v) {
				logger.info("received publication " + sequence);
				publications.add(new orc.orchard.rmi.Publication(
						sequence, new Date(), ((Constant)v).getValue()));
				sequence++;
			}
		});
	}

	public synchronized void start() throws InvalidJobStateException {
		logger.info("start");
		if (!isNew) throw new InvalidJobStateException(state());
		isNew = false;
		engine.debugMode = true;
		engine.start(node, null);
		new Thread(engine).start();
	}

	public synchronized void abort() throws InvalidJobStateException {
		logger.info("abort");
		engine.terminate();
	}

	public JobConfiguration configuration() {
		return configuration;
	}

	public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException {
		throw new UnsupportedFeatureException("Not supported");
	}

	public synchronized List<Publication> publications() throws InvalidJobStateException {
		logger.info("publications");
		publicationBuffer.drainTo(publications);
		return publications;
	}

	public synchronized String state() {
		logger.info("state");
		if (isNew) return "NEW";
		else if (isDead()) return "DEAD";
		else if (engine.isBlocked()) return "WAITING";
		else return "RUNNING";
	}

	public boolean isDead() {
		return engine.isDead();
	}
}