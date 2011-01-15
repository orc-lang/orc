//
// ThreadWaiter.java -- Java class ThreadWaiter
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

/**
 * Use Java wait/notify to implement suspend/resume.
 * @see Waiter
 * @author quark
 */
public class ThreadWaiter implements Waiter {
	private Object monitor;

	@Override
	public void suspend(final Object monitor) throws InterruptedException {
		this.monitor = monitor;
		this.monitor.wait();
	}

	@Override
	public void resume() {
		this.monitor.notify();
	}
}
