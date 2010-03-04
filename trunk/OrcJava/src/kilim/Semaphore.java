//
// Semaphore.java -- Java class Semaphore
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package kilim;

import java.util.LinkedList;
 
/**
 * Same as {@link java.util.concurrent.Semaphore}, but
 * for Kilim tasks.
 * FIXME: {@link kilim.Task#pause(PauseReason)} is package-access-restricted,
 * so we need to put this class in the kilim package to get access to it.
 * @author quark
 */
public class Semaphore {
	/**
	 * Task waiting to acquire the semaphore.
	 */
	private static class Waiter implements PauseReason {
		private final Task task;
		private boolean valid = true;

		public Waiter(final Task task) {
			this.task = task;
		}

		public boolean isValid() {
			return valid;
		}

		public void resume() {
			valid = false;
			task.resume();
		}
	}

	private final LinkedList<Waiter> waiters = new LinkedList<Waiter>();
	private int n;

	public Semaphore(final int n) {
		this.n = n;
	}

	public synchronized void acquire() throws Pausable {
		if (n == 0) {
			final Waiter w = new Waiter(Task.getCurrentTask());
			waiters.add(w);
			Task.pause(w);
		} else {
			--n;
		}
	}

	public synchronized void release() {
		final Waiter w = waiters.poll();
		if (w != null) {
			w.resume();
		} else {
			++n;
		}
	}

	public int observe() {
		return n;
	}
}
