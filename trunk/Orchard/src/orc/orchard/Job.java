package orc.orchard;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.oil.Expr;
import orc.error.TokenException;
import orc.lib.orchard.Prompt.PromptCallback;
import orc.lib.orchard.Prompt.Promptable;
import orc.lib.orchard.Redirect.Redirectable;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.oil.ValueMarshaller;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
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
	 * Thread-safe buffer for events which clients can monitor.
	 * TODO: allow clients to decide which events they are interested in
	 * and ignore all others.
	 */
	private static class EventBuffer {
		/**
		 * Queue publications for retrieval by listen() and events().
		 */
		private LinkedList<JobEvent> buffer = new LinkedList<JobEvent>();
		/**
		 * Requests waiting on events.
		 */
		private WaiterManager waiters = new WaiterManager(this);
		/** Is the queue closed? */
		private boolean closed = false;
		/** Next sequence number for events. */
		private int sequence = 0;
		/** Track the size of the buffer for throttling. */
		private int bufferedSize = 0;
		/** Maximum allowed size of buffer. */
		private int bufferSize = 100;
		/** Number of events which are eligible to be purged. */
		private int purgable = 0;
		public EventBuffer() {}
		/**
		 * Set the maximum allowed size of the buffer.
		 * @param bufferSize
		 */
		public void setBufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
		}
		/**
		 * Add an event to the stream, blocking if the stream is full.
		 */
		public synchronized void add(JobEvent value) {
			while (bufferedSize >= bufferSize) {
				System.out.println("Buffer full.");
				try {
					wait();
				} catch (InterruptedException e) {
					// do nothing
				}
			}
			value.sequence = ++sequence;
			value.timestamp = new Date();
			bufferedSize++;
			buffer.add(value);
			waiters.resume();
		}
		/**
		 * Return buffered events. If necessaray, block (using the provided
		 * waiter) until new events arrive or the stream is closed. Only when
		 * the stream is closed will this return an empty list.
		 */
		public synchronized List<JobEvent> get(Waiter waiter) throws InterruptedException {
			// wait for the buffer to fill or stream to close
			while (buffer.isEmpty() && !closed) {
				waiters.suspend(waiter);
			}
			// return the contents of the buffer
			List<JobEvent> out = new LinkedList<JobEvent>(buffer);
			purgable = out.size();
			return out;
		}
		/**
		 * Discard events which have been returned by get.
		 */
		public synchronized void purge() {
			// Because the buffer is FIFO, we just have
			// to know how many events were returned by get
			// and throw away that many.
			Iterator<JobEvent> it = buffer.iterator();
			for (int i = 0; i < purgable; ++i) {
				assert(it.hasNext());
				it.next();
				it.remove();
				bufferedSize--;
			}
			purgable = 0;
			notify();
		}
		/**
		 * Close the stream. This will send an empty list to any clients listening for new events.
		 */
		public synchronized void close() {
			closed = true;
			waiters.resumeAll();
		}
	}
	
	/** Has start() been called yet? */
	private boolean isNew = true;
	private Date startDate;
	/**
	 * This is stored here in case the job implementation or client needs to
	 * refer to it.
	 */
	private JobConfiguration configuration;
	
	private class JobEngine extends OrcEngine implements Promptable, Redirectable {
		private StringBuffer printBuffer = new StringBuffer();
		/** Close the event stream when done running. */
		@Override
		public void run() {
			super.run();
			events.close();
		}
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
		@Override
		public void pub(Value v) {
			events.add(new PublicationEvent(v.accept(new ValueMarshaller())));
		}
		
		public void prompt(String message, PromptCallback callback) {
			int promptID;
			synchronized(pendingPrompts) {
				promptID = nextPromptID++;
				pendingPrompts.put(promptID, callback);
			}
			events.add(new PromptEvent(promptID, message));
		}

		public void redirect(URL url) {
			events.add(new RedirectEvent(url));
		}
	}
	private int nextPromptID = 1;
	private Map<Integer, PromptCallback> pendingPrompts =
		new HashMap<Integer, PromptCallback>();
	/** The engine will handle all the interesting work of the job. */
	private OrcEngine engine = new JobEngine();
	/** Events which can be monitored. */
	private EventBuffer events;
	private LinkedList<FinishListener> finishers = new LinkedList<FinishListener>();

	protected Job(Expr expression, JobConfiguration configuration) {
		this.configuration = configuration;
		this.events = new EventBuffer();
		Node node = expression.compile();
		//engine.debugMode = true;
		engine.start(node);
	}

	public synchronized void start() throws InvalidJobStateException {
		if (!isNew) throw new InvalidJobStateException(state());
		isNew = false;
		new Thread(engine).start();
	}
	
	public synchronized void finish() {
		halt();
		for (FinishListener finisher : finishers) {
			try {
				finisher.finished(this);
			} catch (RemoteException e) {
				// FIXME: better way to handle this?
				System.err.println("Caught remote exception " + e.toString());
				e.printStackTrace();
			}
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
	 * Return events which occurred since the job started or purgeEvents was last called.
	 * If no events have occurred, block using waiter until one occurs.
	 * If/when the job completes (so no more events can occur), return
	 * an empty list.
	 */
	public List<JobEvent> getEvents(Waiter waiter) throws InterruptedException {
		return events.get(waiter);
	}

	public void purgeEvents() {
		events.purge();
	}

	public synchronized String state() {
		if (isNew) return "NEW";
		else if (engine.isDead()) return "DONE";
		else return "RUNNING";
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public Date getStartDate() {
		return (Date)startDate.clone();
	}

	public void setEventBufferSize(Integer eventBufferSize) {
		if (eventBufferSize != null) events.setBufferSize(eventBufferSize);
	}
	
	/**
	 * Submit a response to a prompt (initiated by the Prompt site).
	 * @throws InvalidPromptException if promptID is invalid
	 */
	public synchronized void respondToPrompt(int promptID, String response) throws InvalidPromptException {
		PromptCallback callback = pendingPrompts.get(promptID);
		if (callback == null) throw new InvalidPromptException();
		pendingPrompts.remove(promptID);
		callback.respondToPrompt(response);
	}

	public synchronized void cancelPrompt(int promptID) throws InvalidPromptException {
		PromptCallback callback = pendingPrompts.get(promptID);
		if (callback == null) throw new InvalidPromptException();
		pendingPrompts.remove(promptID);
		callback.cancelPrompt();
	}
}