//
// Job.java -- Java class Job
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.Handle;
import orc.OrcEvent;
import orc.OrcEventAction;
import orc.OrcOptions;
import orc.ast.oil.nameless.Expression;
import orc.error.OrcException;
import orc.error.runtime.ExecutionException;
import orc.lib.util.PromptCallback;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.events.BrowseEvent;
import orc.orchard.events.JobEvent;
import orc.orchard.events.PrintlnEvent;
import orc.orchard.events.PromptEvent;
import orc.orchard.events.PublicationEvent;
import orc.orchard.events.TokenErrorEvent;
import orc.run.Orc;
import orc.run.StandardOrcRuntime;
import scala.util.parsing.input.Positional;

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
		private final int bufferSize;
		/**
		 * Queue publications for retrieval by listen() and events().
		 */
		private final LinkedList<JobEvent> buffer = new LinkedList<JobEvent>();
		/**
		 * Requests waiting on events.
		 */
		private final WaiterManager waiters = new WaiterManager(this);
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

		public EventBuffer(final int bufferSize) {
			this.bufferSize = bufferSize;
		}

		/**
		 * Add an event to the stream, blocking if the stream is full.
		 * Returns false if interrupted.
		 */
		public synchronized boolean add(final JobEvent value) {
			while (bufferedSize >= bufferSize) {
				System.out.println("Buffer full.");
				try {
					blocked = true;
					wait();
					blocked = false;
				} catch (final InterruptedException e) {
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
		 * Return buffered events. If necessary, block (using the provided
		 * waiter) until new events arrive or the stream is closed. Only when
		 * the stream is closed will this return an empty list.
		 */
		public synchronized List<JobEvent> get(final Waiter waiter) throws InterruptedException {
			// wait for the buffer to fill or stream to close
			while (buffer.isEmpty() && !closed) {
				waiters.suspend(waiter);
			}
			// return the contents of the buffer
			final List<JobEvent> out = new LinkedList<JobEvent>(buffer);
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
			final Iterator<JobEvent> it = buffer.iterator();
			for (int i = 0; i < purgable; ++i) {
				assert it.hasNext();
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

		/**
		 * Is the buffer's producer no longer adding events?
		 */
		public boolean isClosed() {
			return closed;
		}
	}

	private Date startDate;

	public class JobEngine extends StandardOrcRuntime implements Runnable {
		private final StringBuffer printBuffer = new StringBuffer();
		private final Expression expression;
		private final OrcOptions config;

		public JobEngine(final Expression expression, final OrcOptions config) {
			super();
			this.expression = expression;
			this.config = config;
		}

		public Job getJob() {
			return Job.this;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			final JobEventActions jea = new JobEventActions();
			try {
				runSynchronous(expression, jea.asFunction(), config);
			} catch (final OrcException e) {
				jea.caught(e);
			} finally {
				jea.halted();
				stop(); // kill threads and reclaim resources
				Job.this.engine = null;
				Job.this.worker = null;
			}
		}

		class JobEventActions extends OrcEventAction {
			@Override
			public void published(final Object v) {
				events.add(new PublicationEvent(v));
			}

			/** Send token errors to the event stream. */
			@Override
			public void caught(final Throwable e) {
				System.err.println();
				System.err.println("Problem: " + e);
				if (e instanceof Positional) {
					System.err.println("Source location: " + ((Positional) e).pos());
				}
				e.printStackTrace();
				final Throwable cause = e.getCause();
				if (cause != null) {
					System.err.println("Caused by:");
					cause.printStackTrace();
				}
				System.err.println();

				final TokenErrorEvent ee = new TokenErrorEvent(e);
				events.add(ee);
			}

			/** Close the event stream when done running. */
			@Override
			public void halted() {
				// flush the buffer if anything is left
				final String printed = printBuffer.toString();
				if (printed.length() > 0) {
					events.add(new PrintlnEvent(printed));
				}
				events.close();
			}

			/** 
			 * Save prints in a buffer.
			 * Send completed lines to the event stream.
			 */
			@Override
			public void other(final OrcEvent event) throws Exception {
		        if (event instanceof orc.lib.util.PromptEvent) {
		        	orc.lib.util.PromptEvent pe = (orc.lib.util.PromptEvent)event;
		        	int promptID;
					synchronized (pendingPrompts) {
						promptID = nextPromptID++;
						pendingPrompts.put(promptID, pe.callback());
					}
					System.out.println("Queueing prompt event with " + promptID + " " + pe.prompt());
					events.add(new PromptEvent(promptID, pe.prompt()));
		        }
		        else if (event instanceof orc.lib.web.BrowseEvent) {
		        	orc.lib.web.BrowseEvent be = (orc.lib.web.BrowseEvent)event;
		        	System.out.println("Queueing browse event with " + be.url());
		        	events.add(new BrowseEvent(be.url()));
		        }
		        else if (event instanceof orc.lib.str.PrintEvent) {
		        	orc.lib.str.PrintEvent pe = (orc.lib.str.PrintEvent)event;
		        	System.out.println("Queueing print event with " + pe.text());
		        	events.add(new PrintlnEvent(pe.text()));
		        }
		        else {
		        	super.other(event);
		        }
			}

		}

	}

	private int nextPromptID = 1;
	private final Map<Integer, PromptCallback> pendingPrompts = new HashMap<Integer, PromptCallback>();
	/** The engine will handle all the interesting work of the job. */
	private JobEngine engine;
	/** Events which can be monitored. */
	private final EventBuffer events;
	/** Tasks to run when the job finishes. */
	private final LinkedList<FinishListener> finishers = new LinkedList<FinishListener>();
	/** Thread in which the main engine is run. */
	private Thread worker;
	private final String id;

	protected Job(final String id, final Expression expression, final OrcOptions config) throws ExecutionException {
		this.id = id;
		this.events = new EventBuffer(10);
		engine = new JobEngine(expression, config);
	}

	public static Job getJobFromHandle(final Handle callHandle) throws UnsupportedOperationException {
		try {
			final scala.Option<Orc.Token> listener = ((Orc.SiteCallHandle) callHandle).listener();
			if (listener.isDefined()) {
				return ((JobEngine) ((Orc.Token) (listener.get())).runtime()).getJob();
			} else {
				return null;
			}
		} catch (final ClassCastException e) {
			throw new UnsupportedOperationException("This site may be called only from an Orchard JobEngine", e);
		}
	}

	public String getId() {
		return id;
	}

	public synchronized void start() throws InvalidJobStateException {
		if (worker != null || events.isClosed()) {
			throw new InvalidJobStateException(getState());
		}
		worker = new Thread(engine, "Orchard Job " + id);
		worker.start();
	}

	@Override
	public synchronized void finish() {
		halt();
		for (final FinishListener finisher : finishers) {
			try {
				finisher.finished(this);
			} catch (final RemoteException e) {
				// FIXME: better way to handle this?
				System.err.println("Caught remote exception " + e.toString());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Register a listener to be called when the job finishes.
	 */
	public void onFinish(final FinishListener f) {
		finishers.add(f);
	}

	@Override
	public synchronized void halt() {
		if (worker == null) {
			return;
		}
		engine.stop();
	}

	/**
	 * Return events which occurred since the job started or purgeEvents was last called.
	 * If no events have occurred, block using waiter until one occurs.
	 * If/when the job completes (so no more events can occur), return
	 * an empty list.
	 */
	public List<JobEvent> getEvents(final Waiter waiter) throws InterruptedException {
		return events.get(waiter);
	}

	public void purgeEvents() {
		events.purge();
	}

	@Override
	public synchronized String getState() {
		if (worker == null && !events.isClosed()) {
			return "NEW";
		} else if (events.isClosed()) {
			return "DONE";
		} else if (events.isBlocked()) {
			return "BLOCKED";
		} else {
			return "RUNNING";
		}
	}

	public void setStartDate(final Date startDate) {
		this.startDate = startDate;
	}

	@Override
	public Date getStartDate() {
		return (Date) startDate.clone();
	}

	/**
	 * Submit a response to a prompt (initiated by the Prompt site).
	 * @throws InvalidPromptException if promptID is invalid
	 */
	public synchronized void respondToPrompt(final int promptID, final String response) throws InvalidPromptException {
		final PromptCallback callback = pendingPrompts.get(promptID);
		if (callback == null) {
			throw new InvalidPromptException();
		}
		pendingPrompts.remove(promptID);
		callback.respondToPrompt(response);
	}

	public synchronized void cancelPrompt(final int promptID) throws InvalidPromptException {
		final PromptCallback callback = pendingPrompts.get(promptID);
		if (callback == null) {
			throw new InvalidPromptException();
		}
		pendingPrompts.remove(promptID);
		callback.cancelPrompt();
	}

	@Override
	public int getTotalNumEvents() {
		return events.getTotalNumEvents();
	}
}
