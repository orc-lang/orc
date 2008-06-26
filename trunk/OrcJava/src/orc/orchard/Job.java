package orc.orchard;

import java.io.File;
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
	 * TODO: allow clients to decide which events they are interested in
	 * and ignore all others.
	 */
	private static class EventStream {
		/**
		 * Queue publications for retrieval by listen() and events().
		 */
		private LinkedList<JobEvent> buffer = new LinkedList<JobEvent>();
		/**
		 * History of values published via listen().
		 */
		private LinkedList<JobEvent> events = new LinkedList<JobEvent>();
		/**
		 * Requests waiting on events.
		 */
		private WaiterManager waiters = new WaiterManager(this);
		/** Is the queue closed? */
		private boolean closed = false;
		private int sequence = 0;
		/**
		 * Add an event to the stream.
		 */
		public synchronized void add(JobEvent value) {
			value.sequence = ++sequence;
			value.timestamp = new Date();
			buffer.add(value);
			waiters.resume();	
		}
		/**
		 * Return all events added to the stream since the last call
		 * to listen. If necessaray, block (using the provided waiter)
		 * until new events arrive or the stream is closed.
		 * Only when the stream is closed will this return an empty list.
		 */
		public synchronized List<JobEvent> listen(Waiter waiter) throws InterruptedException {
			// wait for the buffer to fill or stream to close
			while (buffer.isEmpty() && !closed) {
				waiters.suspend(waiter);
			}
			// drain and return the contents of the buffer
			events.addAll(buffer);
			List<JobEvent> out = new LinkedList<JobEvent>(buffer);
			buffer.clear();
			return out;
		}
		/**
		 * Return all events added to the stream.
		 */
		public synchronized List<JobEvent> events() {
			List<JobEvent> out = new LinkedList<JobEvent>(events);
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
		private StringBuffer printBuffer = new StringBuffer();
		/** Send token errors to the event stream. */
		@Override
		public void tokenError(Token t, TokenException problem) {
			TokenErrorEvent e = new TokenErrorEvent(problem);
			// TODO: compute a stack trace based on the token's
			// list of callers
			events.add(e);
		}
		/** Save prints in a buffer. */
		@Override
		public void print(String s) {
			synchronized (printBuffer) {
				printBuffer.append(s);
			}
		}
		/** Send printed lines to the event stream. */
		@Override
		public void println(String s) {
			String out; 
			synchronized (printBuffer) {
				printBuffer.append(s);
				out = printBuffer.toString();
				printBuffer = new StringBuffer();
			}
			events.add(new PrintlnEvent(out));
		}
	};
	/** Events which can be monitored. */
	private EventStream events = new EventStream();
	private LinkedList<FinishListener> finishers = new LinkedList<FinishListener>();

	protected Job(String id, Expr expression, JobConfiguration configuration) {
		this.id = id;
		this.configuration = configuration;
		Node node = expression.compile(new Result() {
			public void emit(Value v) {
				events.add(new PublicationEvent(v.marshal()));
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
				events.close();
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

	/**
	 * Return events which occurred since the last call to listen.
	 * If no events have occurred, block using waiter until one occurs.
	 * If/when the job completes (so no more events can occur), return
	 * an empty list.
	 * @see JobInterface.listen
	 */
	public List<JobEvent> listen(Waiter waiter)
		throws UnsupportedFeatureException, InterruptedException
	{
		return events.listen(waiter);
	}

	public List<JobEvent> events() {
		return events.events();
	}

	public List<JobEvent> eventsAfter(int sequence) {
		List<JobEvent> out = new LinkedList<JobEvent>();
		try {
			// sequence numbers are guaranteed to correspond to indices in the
			// list, so we can skip directly to the next sequence number
			ListIterator<JobEvent> it = events().listIterator(sequence);
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