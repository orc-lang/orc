//
// ThreadWaiter.java -- Java class ThreadWaiter
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
 * Use Java wait/notify to implement suspend/resume.
 * 
 * @see Waiter
 * @author quark
 */
public class ThreadWaiter implements Waiter {
    private Object monitorWaitedOn;

    @Override
    public void suspend(final Object monitor) throws InterruptedException {
        monitorWaitedOn = monitor;
        monitorWaitedOn.wait();
    }

    @Override
    public void resume() {
        monitorWaitedOn.notify();
    }
}
