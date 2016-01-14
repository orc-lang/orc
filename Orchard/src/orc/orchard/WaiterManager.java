//
// WaiterManager.java -- Java class WaiterManager
// Project Orchard
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.LinkedList;

/**
 * Manage waiters waiting for the same event. This is NOT thread-safe -- since
 * you should only call it from within a synchronized block there is no need.
 *
 * @author quark
 */
public class WaiterManager {
    private final LinkedList<Waiter> queue = new LinkedList<Waiter>();
    private final Object monitor;

    public WaiterManager(final Object monitor) {
        this.monitor = monitor;
    }

    public void suspend(final Waiter w) throws InterruptedException {
        queue.remove(w);
        queue.add(w);
        w.suspend(monitor);
    }

    public void resume() {
        if (queue.isEmpty()) {
            return;
        }
        queue.removeFirst().resume();
    }

    public void resumeAll() {
        while (!queue.isEmpty()) {
            queue.removeFirst().resume();
        }
    }
}
