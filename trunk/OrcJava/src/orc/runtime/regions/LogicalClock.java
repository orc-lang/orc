//
// LogicalClock.java -- Java class LogicalClock
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.regions;

import java.util.PriorityQueue;

import orc.error.runtime.SiteException;
import orc.runtime.Token;

/**
 * An event queue for site calls to a logical timer (created by MakeTimer).
 * 
 * <p>
 * According to the semantics of logical time, logical timers can only advance
 * when there are no pending site calls except those to so-called "immediate" sites.
 * However it's not so clear how logical timers should interact with "<<".
 * For the moment clocks are allowed to advance even if there are tokens
 * blocked on a group cell.
 * 
 * @author dkitchin, quark
 */
public final class LogicalClock extends SubRegion {
	/** Tokens blocked pending future events. */
	private final PriorityQueue<LtimerQueueEntry> eventQueue;
	/** The current logical time. */
	private int currentTime = 0;
	private final LogicalClock parentClock;

	public LogicalClock(final Region parent, final LogicalClock parentClock) {
		super(parent);
		this.parentClock = parentClock;
		eventQueue = new PriorityQueue<LtimerQueueEntry>();
		currentTime = 0;
	}

	/** Schedule a token to resume at a future time. */
	public final void addEvent(final int delay, final Token token) {
		eventQueue.add(new LtimerQueueEntry(delay + currentTime, token));
		token.setQuiescent();
	}

	/** Advance the logical time. Called when all child tokens and clocks are quiescent. */
	@Override
	protected void maybeDeactivate() {
		if (eventQueue.isEmpty()) {
			// no tokens are ready for the next round:
			// the simulation may be over, or may be blocked
			// waiting for an external event; signal the
			// parent clock that it can advance
			deactivate();
			return;
		} else {
			// Advance the clock to the next logical time and
			// resume all tokens waiting for that time
			LtimerQueueEntry top = eventQueue.peek();
			currentTime = top.time;
			while (top != null && top.time <= currentTime) {
				final Token token = eventQueue.remove().token;
				token.unsetQuiescent();
				token.resume();
				top = eventQueue.peek();
			}
		}
	}

	/**
	 * Class representing Ltimer Queue Entry 
	 * @author dkitchin, based on RtimerQueueEntry by jayeshs
	 */
	private static class LtimerQueueEntry implements Comparable<LtimerQueueEntry> {
		public int time;
		public Token token;

		public LtimerQueueEntry(final int time, final Token token) {
			this.token = token;
			this.time = time;
		}

		public int compareTo(final LtimerQueueEntry n) {
			// sort the queue items earliest first
			return Integer.signum(time - n.time);
		}
	}

	public LogicalClock getParentClock() throws SiteException {
		if (parentClock == null) {
			throw new SiteException("Cannot pop the root logical clock.");
		}
		return parentClock;
	}

	public int getCurrentTime() {
		return currentTime;
	}
}
