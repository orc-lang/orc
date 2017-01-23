//
// Waiter.java -- Java class Waiter
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

/**
 * Provides blocking or callback behavior for asynchronous methods.
 * <p>
 * In a servlet, it's bad practice to actually block a method call because
 * threads are a limited resource. As an alternative, some servlet containers
 * allow you to register a request to be suspended and later resumed/retried
 * (examples: Jetty Continuations, Java Servlet 3.0 suspendable requests).
 * <p>
 * This interface abstracts away the differences between blocking and callback
 * implementations of asynchrony so the underlying implementation doesn't have
 * to know about them.
 * <p>
 * This is not thread-safe -- you are expected to call it from within a block
 * synchronized on the monitor.
 *
 * @author quark
 */
public interface Waiter {
    /**
     * Suspend the current request and release locks. When the request is
     * resumed, any of the following may happen:
     * <ul>
     * <li>suspend returns.
     * <li>suspend throws InterruptedException.
     * <li>Call is retried with the same Waiter.
     * </ul>
     * Therefore the request <i>must be reentrant</i>.
     *
     * @param monitor the monitor to wait on if a blocking implementation is
     *            used.
     * @throws InterruptedException if the call timed out.
     */
    public void suspend(Object monitor) throws InterruptedException;

    /**
     * Signal that the asynchronous call should be resumed. This may mean that a
     * waiting call to suspend() returns, or that the asynchronous call is
     * simply retried.
     */
    public void resume();
}
