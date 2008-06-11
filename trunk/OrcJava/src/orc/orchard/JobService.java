package orc.orchard;

import java.net.URI;
import java.rmi.Naming;
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

/**
 * Standard implementation of a JobService. Extenders should only need to
 * provide a constructor and override onFinish() to clean up any external
 * resources.
 * 
 * @author quark
 * 
 */
public abstract class JobService<JC extends JobConfiguration, P extends Publication> implements orc.orchard.interfaces.JobService<JC, P> {
	/** Has start() been called yet? */
	private boolean isNew = true;
	/**
	 * This is stored here in case the job implementation or client needs to
	 * refer to it.
	 */
	private JC configuration;
	/** The engine will handle all the interesting work of the job. */
	private OrcEngine engine = new OrcEngine();
	/**
	 * Queue publications for retrieval by listen() and publications(). Values
	 * are published directly from the engine to this buffer. listen() will wait
	 * for this buffer to become non-empty, then drain it to pubs and return the
	 * list of drained publications. publications() will return pubs with
	 * pubsBuff concatenated. This object also serves as the lock/monitor for
	 * all modifications to pubsBuff and pubs.
	 * 
	 * Initially this was a BlockingQueue but that did not provide sufficient
	 * control over locking.
	 */
	private List<P> pubsBuff = new LinkedList<P>();
	/**
	 * History of values published via listen().
	 */
	private List<P> pubs = new LinkedList<P>();
	/** Used for debugging. */
	private Logger logger;
	
	/**
	 * Override this to handle any setup which needs to occur when the job is
	 * started.
	 */
	protected JobService(Logger logger, JC configuration, Expression expression) {
		this.configuration = configuration;
		this.logger = logger;
		Node node = expression.compile(new Result() {
			int sequence = 1;
			public void emit(Value v) {
				JobService.this.logger.info("received publication " + sequence);
				synchronized (pubsBuff) {
					pubsBuff.add(createPublication(
							sequence++, new Date(), ((Constant)v).getValue()));
					pubsBuff.notify();
				}
			}
		});
		engine.debugMode = true;
		engine.start(node, null);
	}
	
	protected abstract P createPublication(int sequence, Date date, Object value);
	
	/**
	 * Override this to handle any cleanup which needs to occur when the job is
	 * successfully finished (via finish() or abort()).
	 * 
	 * @throws RemoteException
	 */
	protected abstract void onFinish() throws RemoteException;

	public synchronized void start() throws InvalidJobStateException {
		logger.info("start");
		if (!isNew) throw new InvalidJobStateException(state());
		isNew = false;
		new Thread(engine).start();
	}
	
	public synchronized void finish() throws InvalidJobStateException, RemoteException {
		logger.info("finish");
		if (!engine.isDead()) throw new InvalidJobStateException(state());
		onFinish();
	}

	public synchronized void abort() throws RemoteException {
		logger.info("abort");
		engine.terminate();
		onFinish();
	}

	public JC configuration() {
		return configuration;
	}

	public List<P> listen() throws InvalidJobStateException, UnsupportedFeatureException {
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
			List<P> out = new LinkedList<P>(pubsBuff);
			pubsBuff.clear();
			return out;
		}
	}

	public List<P> publications() throws InvalidJobStateException {
		synchronized (pubsBuff) {
			logger.info("publications");
			List<P> out = new LinkedList<P>(pubs);
			out.addAll(pubsBuff);
			return out;
		}
	}

	public List<P> publicationsAfter(int sequence) throws InvalidJobStateException {
		logger.info("publications(" + sequence + ")");
		List<P> out = new LinkedList<P>();
		try {
			// sequence numbers are guaranteed to correspond to indices in the
			// list, so we can skip directly to the next sequence number
			ListIterator<P> it = publications().listIterator(sequence);
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
		else if (engine.isDead()) return "DONE";
		else if (engine.isBlocked()) return "WAITING";
		else return "RUNNING";
	}
}