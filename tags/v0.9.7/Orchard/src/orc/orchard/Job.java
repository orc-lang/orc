package orc.orchard;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.Config;
import orc.ast.oil.Compiler;
import orc.ast.oil.Expr;
import orc.error.runtime.TokenException;
import orc.lib.orchard.Prompt.PromptCallback;
import orc.lib.orchard.Prompt.Promptable;
import orc.lib.orchard.Redirect.Redirectable;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.events.JobEvent;
import orc.orchard.events.PrintlnEvent;
import orc.orchard.events.PromptEvent;
import orc.orchard.events.PublicationEvent;
import orc.orchard.events.RedirectEvent;
import orc.orchard.events.TokenErrorEvent;
import orc.orchard.values.ValueMarshaller;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Pub;

/**
 * Standard implementation of a JobService. Extenders should only need to
 * provide a constructor and override onFinish() to clean up any external
 * resources.
 * 
 * @author quark
 */
public final class Job implements JobMBean {
	/**
	 * Used to handle cleanup when the job finishes.
	 */
	public interface FinishListener {
		public void finished(Job job) throws RemoteException;
	}
	
	/**
	 * Thread-safe buffer for events which clients can monitor.
	 * TODO: allow clients to decide which events they are interested in
	 * and ignore all others.
	 */
	private static class EventBuffer {
		/** Maximum allowed size of buffer. */
		private int bufferSize;
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
		/** Is the queue full? */
		private boolean blocked = false;
		/** Next sequence number for events. */
		private int sequence = 0;
		/** Track the size of the buffer for throttling. */
		private int bufferedSize = 0;
		/** Number of events which are eligible to be purged. */
		private int purgable = 0;
		public EventBuffer(int bufferSize) {
			this.bufferSize = bufferSize;
		}
		
		/**
		 * Add an event to the stream, blocking if the stream is full.
		 * Returns false if interrupted.
		 */
		public synchronized boolean add(JobEvent value) {
			while (bufferedSize >= bufferSize) {
				System.out.println("Buffer full.");
				try {
					blocked = true;
					wait();
					blocked = false;
				} catch (InterruptedException e) {
					return false;
				}
			}
			value.sequence = ++sequence;
			value.timestamp = new Date();
			bufferedSize++;
			buffer.add(value);
			waiters.resume();
			return true;
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
		public synchronized int getTotalNumEvents() {
			return sequence;
		}
		/**
		 * Is the queue blocked because it is full?
		 */
		public synchronized boolean isBlocked() {
			return blocked;
		}
	}
	private Date startDate;
	
	private class JobEngine extends OrcEngine
	implements Promptable, Redirectable {
		public JobEngine(Config config) {
			super(config);
		}

		private StringBuffer printBuffer = new StringBuffer();
		/** Close the event stream when done running. */
		@Override
		public void run() {
			super.run();
			// flush the buffer if anything is left
			String printed = printBuffer.toString();
			if (printed.length() > 0) events.add(new PrintlnEvent(printed));
			events.close();
		}
		/** Send token errors to the event stream. */
		@Override
		public void tokenError(TokenException problem) {
			System.err.println();
			System.err.println("Problem: " + problem);
			System.err.println("Source location: " + problem.getSourceLocation());
			problem.printStackTrace();
			Throwable cause = problem.getCause();
			if (cause != null) {
				System.err.println("Caused by:");
				cause.printStackTrace();
			}
			System.err.println();
			
			TokenErrorEvent e = new TokenErrorEvent(problem);
			// TODO: compute a stack trace based on the token's
			// list of callers
			events.add(e);
		}
		/** 
		 * Save prints in a buffer.
		 * Send completed lines to the event stream.
		 */
		@Override
		public void print(String s, boolean newline) {
			String out = null; 
			synchronized (printBuffer) {
				printBuffer.append(s);
				if (newline) {
					out = printBuffer.toString();
					printBuffer = new StringBuffer();
				}
			}
			if (newline) {
				events.add(new PrintlnEvent(out));
			}
		}
		@Override
		public void publish(Object v) {
			events.add(new PublicationEvent(ValueMarshaller.visit(new ValueMarshaller(), v)));
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
	private final Map<Integer, PromptCallback> pendingPrompts =
		new HashMap<Integer, PromptCallback>();
	/** The engine will handle all the interesting work of the job. */
	private final OrcEngine engine;
	/** Events which can be monitored. */
	private final EventBuffer events;
	/** Tasks to run when the job finishes. */
	private final LinkedList<FinishListener> finishers = new LinkedList<FinishListener>();
	/** Thread in which the main engine is run. */
	private Thread worker;

	protected Job(Expr expression, Config config) {
		this.events = new EventBuffer(10);
		engine = new JobEngine(config);
		Node node = Compiler.compile(expression, new Pub());
		//engine.debugMode = true;
		engine.start(node);
	}

	public synchronized void start() throws InvalidJobStateException {
		if (worker != null) throw new InvalidJobStateException(getState());
		worker = new Thread(engine);
		worker.start();
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
		if (worker == null) return;
		engine.terminate();
		// if the engine is blocked, interrupt it
		// so it can halt
		worker.interrupt();
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

	public synchronized String getState() {
		if (worker == null) return "NEW";
		else if (engine.isDead()) return "DONE";
		else if (events.isBlocked()) return "BLOCKED";
		else return "RUNNING";
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public Date getStartDate() {
		return (Date)startDate.clone();
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
	public int getTotalNumEvents() {
		return events.getTotalNumEvents();
	}
}
