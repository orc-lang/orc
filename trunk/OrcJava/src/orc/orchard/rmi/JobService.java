package orc.orchard.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
	/**
	 * Queue publications for retrieval by listen() and publications(). At one
	 * point this was a blocking queue but that did not give me sufficient
	 * control over the locking.
	 */
	private List<Publication> pubsBuff = new LinkedList<Publication>();
	/** History of values published via listen(). */
	private List<Publication> pubs = new LinkedList<Publication>();
	private Logger logger;
	
	public JobService(JobConfiguration configuration, Expression expression, Logger logger_) throws RemoteException {
		super();
		this.configuration = configuration;
		this.logger = logger_;
		this.node = expression.compile(new Result() {
			int sequence = 1;
			public void emit(Value v) {
				logger.info("received publication " + sequence);
				synchronized (pubsBuff) {
					pubsBuff.add(new orc.orchard.rmi.Publication(
							sequence++, new Date(), ((Constant)v).getValue()));
					pubsBuff.notify();
				}
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
		synchronized (pubsBuff) {
			// wait for the buffer to fill up
			while (pubsBuff.isEmpty()) {
				try {
					pubsBuff.wait();
					break;
				} catch (InterruptedException e) {
					// keep waiting
				}
			}
			// remember these publications for later
			pubs.addAll(pubsBuff);
			// drain the buffer
			List<Publication> out = new LinkedList<Publication>(pubsBuff);
			pubsBuff.clear();
			return out;
		}
	}

	public List<Publication> publications() throws InvalidJobStateException {
		synchronized (pubsBuff) {
			logger.info("publications");
			List<Publication> out = new LinkedList<Publication>(pubs);
			out.addAll(pubsBuff);
			return out;
		}
	}

	public List<Publication> publications(int sequence) throws InvalidJobStateException {
		logger.info("publications(" + sequence + ")");
		List<Publication> out = new LinkedList<Publication>();
		try {
			// sequence numbers are guaranteed to correspond to indices in the
			// list, so we can skip directly to the next sequence number
			ListIterator<Publication> it = publications().listIterator(sequence);
			// copy all of the subsequent publications into the output list
			while (it.hasNext()) out.add(it.next());
		} catch (IndexOutOfBoundsException e) {
			// ignore this error
		}
		return out;
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