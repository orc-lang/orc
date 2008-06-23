package orc.orchard;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import orc.ast.oil.Expr;
import orc.error.TokenException;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
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
	
	/**
	 * Thread-safe queue for events which clients can listen for. Currently
	 * includes publications and token errors.
	 */
	private static class EventStream<V> {
		/**
		 * Queue publications for retrieval by listen() and events().
		 */
		private LinkedList<V> buffer = new LinkedList<V>();
		/**
		 * History of values published via listen().
		 */
		private LinkedList<V> events = new LinkedList<V>();
		/**
		 * Requests waiting on events.
		 */
		private WaiterManager waiters = new WaiterManager(this);
		/** Is the queue closed? */
		private boolean closed = false;
		/**
		 * Add an event to the stream.
		 */
		public synchronized void add(V value) {
			buffer.add(value);
			waiters.resume();	
		}
		/**
		 * Return all events added to the stream since the last call
		 * to listen. If necessaray, block (using the provided waiter)
		 * until new events arrive or the stream is closed.
		 * Only when the stream is closed will this return an empty list.
		 */
		public synchronized List<V> listen(Waiter waiter) throws InterruptedException {
			// wait for the buffer to fill or stream to close
			while (buffer.isEmpty() && !closed) {
				waiters.suspend(waiter);
			}
			// drain and return the contents of the buffer
			events.addAll(buffer);
			List<V> out = new LinkedList<V>(buffer);
			buffer.clear();
			return out;
		}
		/**
		 * Return all events added to the stream.
		 */
		public synchronized List<V> events() {
			List<V> out = new LinkedList<V>(events);
			out.addAll(buffer);
			return out;
		}
		/**
		 * Close the stream. This will send an empty list to any clients listening for new events.
		 */
		public synchronized void close() {
			closed = true;
			waiters.resumeAll();
		}
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
	private OrcEngine engine = new OrcEngine() {
		public void tokenError(Token t, TokenException problem) {
			TokenError e = new TokenError();
			e.location = problem.getSourceLocation();
			e.message = problem.getMessage();
			e.timestamp = new Date();
			// TODO: compute a stack trace based on the token's
			// list of callers
			errors.add(e);
		}
	};
	/** Values published to the engine's top level */
	private EventStream<Publication> publications = new EventStream<Publication>();
	/** Token errors from the engine. */
	private EventStream<TokenError> errors = new EventStream<TokenError>();
	private LinkedList<FinishListener> finishers = new LinkedList<FinishListener>();

	protected Job(String id, Expr expression, JobConfiguration configuration) {
		this.id = id;
		this.configuration = configuration;
		Node node = expression.compile(new Result() {
			int sequence = 1;
			public synchronized void emit(Value v) {
				publications.add(new Publication(sequence++, new Date(), v.marshal()));
			}
		});
		//engine.debugMode = true;
		engine.start(node);
	}

	public synchronized void start() throws InvalidJobStateException {
		if (!isNew) throw new InvalidJobStateException(state());
		isNew = false;
		new Thread(new Runnable() {
			public void run() {
				engine.run();
				// when the engine is complete, notify
				// anybody waiting for further publications or errors
				publications.close();
				errors.close();
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

	public List<Publication> nextPublications(Waiter waiter)
		throws UnsupportedFeatureException, InterruptedException
	{
		return publications.listen(waiter);
	}

	public List<Publication> publications() {
		return publications.events();
	}
	
	public List<TokenError> nextErrors(Waiter waiter)
		throws UnsupportedFeatureException, InterruptedException
	{
		return errors.listen(waiter);
	}
	
	public List<TokenError> errors() {
		return errors.events();
	}

	public List<Publication> publicationsAfter(int sequence) {
		List<Publication> out = new LinkedList<Publication>();
		try {
			// sequence numbers are guaranteed to correspond to indices in the
			// list, so we can skip directly to the next sequence number
			ListIterator<Publication> it = publications.events().listIterator(sequence);
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