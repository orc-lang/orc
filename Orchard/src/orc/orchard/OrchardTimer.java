//
// OrchardTimer.java -- Java class OrchardTimer
// Project Orchard
//
// Created by jthywiss on Dec 9, 2011.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.logging.Logger;

/**
 * A single global timer queue for an Orchard instance. The associated thread
 * terminates when the queue is empty, and starts when needed.
 * <p>
 * Note the timer thread is a daemon thread: If only daemon threads are running
 * in a JVM, it will exit. This means timer tasks might not be run if the JVM is
 * exiting, or the task may be aborted as it is running. Therefore, this timer
 * should <b>not</b> be used for tasks that modify the persistent state outside
 * the JVM (file writes, for example).
 * <p>
 * Note the assumption about the callers' behavior; namely, that callers do not
 * add tasks as a result of the queue emptying or other high-frequency non-empty
 * -> empty -> non-empty cycling.
 * <p>
 * The interface is similar to a subset of <code>java.util.Timer</code>.
 *
 * @see java.util.Timer
 * @author jthywiss
 */
public class OrchardTimer {
    protected static final PriorityQueue<Task> taskQueue = new PriorityQueue<Task>();
    protected static OrchardTimerThread taskRunner;
    protected static Logger logger = Logger.getLogger("orc.orchard.OrchardTimer");

    protected static synchronized void initThreadIfNeeded() {
        if (taskRunner == null) {
            logger.finest("OrchardTimer.initThreadIfNeeded: creating task runner");
            taskRunner = new OrchardTimerThread();
            taskRunner.start();
        }
    }

    public static void schedule(final Task newTask, final long delay_ms) {
        if (delay_ms < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }
        newTask.scheduledTime = System.currentTimeMillis() + delay_ms;
        logger.fine("OrchardTimer.schedule: adding a task to run in " + delay_ms + " ms");
        synchronized (taskQueue) {
            taskQueue.add(newTask);
            initThreadIfNeeded();
            if (taskQueue.peek() == newTask) {
                synchronized (taskRunner) {
                    taskRunner.notifyAll();
                }
            }
        }
    }

    public static void cancel(final Task task) {
        synchronized (taskQueue) {
            final boolean wasHead = taskQueue.peek() == task;
            taskQueue.remove(task);
            if (wasHead) {
                synchronized (taskRunner) {
                    taskRunner.notifyAll();
                }
            }
        }
    }

    public abstract static class Task implements Runnable, Comparable<Task> {
        protected long scheduledTime;

        @Override
        public int compareTo(final Task that) {
            return this.scheduledTime < that.scheduledTime ? -1 : this.scheduledTime == that.scheduledTime ? 0 : 1;
        }

        public void cancel() {
            OrchardTimer.cancel(this);
        }

    }

    protected static class OrchardTimerThread extends Thread {

        public OrchardTimerThread() {
            super("OrchardTimer");
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (!taskQueue.isEmpty()) {
                    final long timeToFirst = taskQueue.element().scheduledTime - System.currentTimeMillis();
                    Task currTask = null;
                    logger.finest("OrchardTimerThread.run: waiting " + timeToFirst + " ms");
                    if (timeToFirst > 0) {
                        synchronized (this) {
                            wait(timeToFirst);
                        }
                    }
                    synchronized (taskQueue) {
                        if (taskQueue.element().scheduledTime <= System.currentTimeMillis()) {
                            currTask = taskQueue.remove();
                        }
                    }
                    if (currTask != null) {
                        logger.finer("OrchardTimerThread.run: running a task");
                        currTask.run();
                    }
                }
            } catch (final NoSuchElementException e) {
                /* Queue has been emptied, just exit */
            } catch (final InterruptedException e) {
                /* Set flag and exit */
                Thread.currentThread().interrupt();
            } finally {
                logger.finer("OrchardTimerThread.run: exiting");
                synchronized (taskQueue) {
                    taskQueue.clear();
                }
                synchronized (OrchardTimer.class) {
                    taskRunner = null;
                }
            }
        }

    }
}
