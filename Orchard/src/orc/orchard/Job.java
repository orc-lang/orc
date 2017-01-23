//
// Job.java -- Java class Job
// Project Orchard
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import scala.util.parsing.input.Positional;

import orc.Handle;
import orc.OrcEvent;
import orc.OrcEventAction;
import orc.OrcOptions;
import orc.ast.oil.nameless.Expression;
import orc.lib.util.PromptCallback;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidPromptException;
import orc.orchard.events.BrowseEvent;
import orc.orchard.events.JobEvent;
import orc.orchard.events.PrintlnEvent;
import orc.orchard.events.PromptEvent;
import orc.orchard.events.PublicationEvent;
import orc.orchard.events.TokenErrorEvent;
import orc.run.StandardOrcRuntime;
import orc.run.core.ExternalSiteCallHandle;
import orc.run.core.Execution;

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
     * Thread-safe buffer for events which clients can monitor. TODO: allow
     * clients to decide which events they are interested in and ignore all
     * others.
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
         * Add an event to the stream, blocking if the stream is full. Returns
         * false if interrupted.
         */
        public synchronized boolean add(final JobEvent value) {
            while (bufferedSize >= bufferSize) {
                logger.finest("Orchard Job event buffer full; caller thread will block");
                try {
                    blocked = true;
                    wait(); //FIXME: Wait until timeout, then terminate job
                    blocked = false;
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
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
         * Close the stream. This will send an empty list to any clients
         * listening for new events.
         */
        public synchronized void close() {
            closed = true;
            waiters.resumeAll();
        }

        public synchronized int size() {
            return bufferedSize;
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
    private Date endDate;

    public class JobEngine extends StandardOrcRuntime {
        private final StringBuffer printBuffer = new StringBuffer();
        private final Expression expression;
        private final OrcOptions config;

        public JobEngine(final Expression expression, final OrcOptions config, final String engineInstanceName) {
            super(engineInstanceName);
            this.expression = expression;
            this.config = config;
        }

        public Job getJob() {
            return Job.this;
        }

        public void run() {
            final JobEventActions jea = new JobEventActions();
            try {
                run(expression, jea.asFunction(), config);
//            } catch (final InterruptedException e) {
//                Thread.currentThread().interrupt();
            } catch (final Throwable e) {
                jea.caught(e);
                stop();
            }
        }

        public void stop() {
            // flush the buffer if anything is left
            final String printed = printBuffer.toString();
            if (printed.length() > 0) {
                events.add(new PrintlnEvent(printed));
            }
            super.stop(); // kill threads and reclaim resources
            events.close();
            Job.this.engine = null;
        }

        class JobEventActions extends OrcEventAction {
            @Override
            public void published(final Object v) {
                logger.finer("Orchard job \"" + id + "\": Queuing publication event with " + v);
                events.add(new PublicationEvent(v));
            }

            /** Send token errors to the event stream. */
            @Override
            public void caught(final Throwable e) {
                if (e instanceof Positional) {
                    logger.log(Level.FINE, "Orchard job \"" + id + "\": Orc program exception at " + ((Positional) e).pos(), e);
                } else {
                    logger.log(Level.FINE, "Orchard job \"" + id + "\": Orc program exception", e);
                }

                final TokenErrorEvent ee = new TokenErrorEvent(e);
                events.add(ee);
            }

            /** Shut down the job engine when done running. */
            @Override
            public void haltedOrKilled() {
                logger.log(Level.FINE, "Orchard job \"" + id + "\": Orc program halted or killed");
                stop();
            }

            /**
             * Save prints in a buffer. Send completed lines to the event
             * stream.
             */
            @Override
            public void other(final OrcEvent event) throws Exception {
                if (event instanceof orc.lib.util.PromptEvent) {
                    final orc.lib.util.PromptEvent pe = (orc.lib.util.PromptEvent) event;
                    final int promptID;
                    synchronized (pendingPrompts) {
                        promptID = nextPromptID++;
                        pendingPrompts.put(promptID, pe.callback());
                    }
                    logger.finer("Orchard job \"" + id + "\": Queuing prompt event with " + promptID + " " + pe.prompt());
                    events.add(new PromptEvent(promptID, pe.prompt()));
                } else if (event instanceof orc.lib.web.BrowseEvent) {
                    final orc.lib.web.BrowseEvent be = (orc.lib.web.BrowseEvent) event;
                    logger.finer("Orchard job \"" + id + "\": Queuing browse event with " + be.url());
                    events.add(new BrowseEvent(be.url()));
                } else if (event instanceof orc.lib.str.PrintEvent) {
                    final orc.lib.str.PrintEvent pe = (orc.lib.str.PrintEvent) event;
                    logger.finer("Orchard job \"" + id + "\": Queuing print event with " + pe.text());
                    events.add(new PrintlnEvent(pe.text()));
                } else {
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
    private final String id;
    private final Account owner;
    protected static Logger logger = Logger.getLogger("orc.orchard");

    protected Job(final String id, final Expression expression, final OrcOptions config, final Account owner) {
        this.id = id;
        this.owner = owner;
        logger.fine("Orchard job \"" + id + "\": Created, user = " + getOwner());
        this.events = new EventBuffer(10);
        engine = new JobEngine(expression, config, "Orchard Job " + id);
    }

    public static Job getJobFromHandle(final Handle callHandle) throws UnsupportedOperationException {
        try {
            return ((JobEngine) ((ExternalSiteCallHandle) callHandle).caller().runtime()).getJob();
        } catch (final ClassCastException e) {
            throw new UnsupportedOperationException("This site may be called only from an Orchard JobEngine", e);
        }
    }

    public String getId() {
        return id;
    }

    /**
     * Begin executing the job.
     *
     * @throws InvalidJobStateException if the job was already started, or was
     *             aborted.
     */
    public synchronized void start() throws InvalidJobStateException {
        if (engine == null || events.isClosed()) {
            throw new InvalidJobStateException(getState());
        }
        engine.run();
    }

    /**
     * Indicate that the client is done with the job. The job will be canceled
     * if necessary.
     * <p>
     * Once this method is called, the service provider is free to garbage
     * collect the service and the service URL may become invalid, so no other
     * methods should be called after this.
     */
    @Override
    public synchronized void finish() {
        cancel();
        for (final FinishListener finisher : finishers) {
            try {
                finisher.finished(this);
            } catch (final RemoteException e) {
                logger.log(Level.SEVERE, "Orchard job \"" + id + "\": Caught remote exception", e);
            }
        }
    }

    /**
     * Register a listener to be called when the job finishes.
     */
    public void onFinish(final FinishListener f) {
        finishers.add(f);
    }

    /**
     * Cancel the job forcibly.
     */
    @Override
    public synchronized void cancel() {
        endDate = new Date();
        logger.fine("Orchard job \"" + id + "\": Finished/canceled");
        if (engine != null) {
            engine.stop();
        }
    }

    /**
     * Return events which occurred since the job started or purgeEvents was
     * last called. If no events have occurred, block using waiter until one
     * occurs. If/when the job completes (so no more events can occur), return
     * an empty list.
     *
     * @throws InterruptedException if the request times out.
     */
    public List<JobEvent> getEvents(final Waiter waiter) throws InterruptedException {
        return events.get(waiter);
    }

    /**
     * Purge all events from the event buffer which have been returned by
     * jobEvents. The client is responsible for calling this method regularly to
     * keep the event buffer from filling up.
     */
    public void purgeEvents() {
        events.purge();
    }

    /**
     * What is the job's state? Possible return values: NEW: not yet started.
     * RUNNING: started and processing tokens. BLOCKED: blocked because event
     * buffer is full. DONE: finished executing.
     * 
     * @return the current state of the job.
     */
    @Override
    public synchronized String getState() {
        if (engine == null && !events.isClosed()) {
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
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String getRunningTime() {
        final long elapsed = (events.isClosed() ? endDate.getTime() : System.currentTimeMillis()) - startDate.getTime();
        return elapsed / 86400000 + " d " + elapsed / 3600000 % 24 + " hr " + elapsed / 60000 % 60 + " min " + elapsed / 1000 % 60 + " s";
    }

    /**
     * Submit a response to a prompt (initiated by the Prompt site).
     * 
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

    /**
     * Cancel a prompt (initiated by the Prompt site).
     * 
     * @throws InvalidPromptException if the promptID is not valid.
     */
    public synchronized void cancelPrompt(final int promptID) throws InvalidPromptException {
        final PromptCallback callback = pendingPrompts.get(promptID);
        if (callback == null) {
            throw new InvalidPromptException();
        }
        pendingPrompts.remove(promptID);
        callback.cancelPrompt();
    }

    @Override
    public String getOwner() {
        return owner.getUsername();
    }

    @Override
    public int getTotalNumEvents() {
        return events.getTotalNumEvents();
    }

    @Override
    public int getNumBufferedEvents() {
        return events.size();
    }

    @Override
    public int getNumPendingPrompts() {
        return pendingPrompts.size();
    }

    @Override
    public int getTokenCount() {
        if (engine.roots().size() != 1)
            return -1;
        Execution soleRoot = (Execution)engine.roots().iterator().next();
        return soleRoot.tokenCount().get();
    }

}
