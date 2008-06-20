package orc.orchard;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import orc.ast.oil.Expr;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
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
public final class Job {
	/**
	 * Used to handle cleanup when the job finishes.
	 */
	public static interface FinishListener {
		public void finished(Job job) throws RemoteException;
	}
	
	/** External identifier for this job. */
	private String id;
	/** URI for this job. */
	private URI uri;
	/** Has start() been called yet? */
	private boolean isNew = true;
	/**
	 * This is stored here in case the job implementation or client needs to
	 * refer to it.
	 */
	private JobConfiguration configuration;
	/** The engine will handle all the interesting work of the job. */
	private OrcEngine engine = new OrcEngine();
	/**
	 * Queue publications for retrieval by listen() and publications(). Values
	 * are published directly from the engine to this buffer. listen() will wait
	 * for this buffer to become non-empty, then drain it to pubs and return the
	 * list of drained publications. publications() will return pubs with
	 * pubsBuff concatenated. This object also serves as the lock for
	 * all modifications to pubsBuff, pubs, and pubsWaiters.
	 * 
	 * Initially this was a BlockingQueue but that did not provide sufficient
	 * control over locking.
	 */
	private LinkedList<Publication> pubsBuff = new LinkedList<Publication>();
	/**
	 * History of values published via listen().
	 */
	private LinkedList<Publication> pubs = new LinkedList<Publication>();
	/**
	 * Requests waiting on publications.
	 */
	private WaiterManager pubsWaiters = new WaiterManager(pubsBuff);
	private LinkedList<FinishListener> finishers = new LinkedList<FinishListener>();

	protected Job(String id, Expr expression, JobConfiguration configuration) {
		this.id = id;
		this.configuration = configuration;
		Node node = expression.compile(new Result() {
			int sequence = 1;
			public void emit(Value v) {
				synchronized (pubsBuff) {
					pubsBuff.add(new Publication(sequence++, new Date(), v.marshal()));
					pubsWaiters.resume();
				}
			}
		});
		engine.debugMode = true;
		engine.start(node);
	}

	public synchronized void start() throws InvalidJobStateException {
		if (!isNew) throw new InvalidJobStateException(state());
		isNew = false;
		new Thread(new Runnable() {
			public void run() {
				engine.run();
				// when the engine is complete, notify
				// anybody waiting for further publications
				synchronized (pubsBuff) {
					pubsWaiters.resumeAll();
				}
			}
		}).start();
	}
	
	public synchronized void finish() throws InvalidJobStateException, RemoteException {
		halt();
		for (FinishListener finisher : finishers) {
			finisher.finished(this);
		}
	}
	
	/**
	 * Register a listener to be called when the job finishes.
	 */
	public void onFinish(FinishListener f) {
		finishers.add(f);
	}

	public synchronized void halt() {
		engine.terminate();
	}

	public JobConfiguration configuration() {
		return configuration;
	}

	public List<Publication> listen(Waiter waiter)
		throws InvalidJobStateException, UnsupportedFeatureException, InterruptedException
	{
		synchronized (pubsBuff) {
			// wait for the buffer to fill or engine to complete
			while (pubsBuff.isEmpty() && !engine.isDead()) {
				pubsWaiters.suspend(waiter);
			}
			// drain and return the contents of the buffer
			pubs.addAll(pubsBuff);
			List<Publication> out = new LinkedList<Publication>(pubsBuff);
			pubsBuff.clear();
			return out;
		}
	}

	public List<Publication> publications() throws InvalidJobStateException {
		synchronized (pubsBuff) {
			List<Publication> out = new LinkedList<Publication>(pubs);
			out.addAll(pubsBuff);
			return out;
		}
	}

	public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException {
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
		if (isNew) return "NEW";
		else if (engine.isDead()) return "DONE";
		else if (engine.isBlocked()) return "WAITING";
		else return "RUNNING";
	}
	
	public String getID() {
		return id;
	}

	public void setURI(URI uri) {
		this.uri = uri;
	}
	
	public URI getURI() {
		return uri;
	}
}