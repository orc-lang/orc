package orc.runtime;

import java.util.PriorityQueue;

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
public final class LogicalClock {
	/** Tokens blocked pending future events. */
	private PriorityQueue<LtimerQueueEntry> eventQueue;
	/** Number of tokens and sub-clocks still active. */
	private int active = 0;
	/** Is this clock quiescent (no active or pending tokens) */
	private boolean quiescent;
	/** The current logical time. */
	int currentTime = 0;
	/** Logical timers form a tree structure. As long as children
	 * are not quiescent, the parent timer cannot advance. */
	LogicalClock parent;
	
	LogicalClock(LogicalClock parent) {
		this.parent = parent;
		eventQueue = new PriorityQueue<LtimerQueueEntry>();
		currentTime = 0;
		unsetQuiescent();
	}
	
	/** Schedule a token to resume at a future time. */
	void addEvent(int delay, Token token) {
		eventQueue.add(new LtimerQueueEntry(delay + currentTime, token));
		token.setQuiescent();
	}
	
	/** Called when this clock is quiescent. */
	private void setQuiescent() {
		quiescent = true;
		if (parent != null) parent.removeActive();
	}
	
	/** Called when this clock becomes not quiescent. */
	private void unsetQuiescent() {
		quiescent = false;
		if (parent != null) parent.addActive();
	}
	
	/** Advance the logical time. Called when all child tokens and clocks are quiescent. */
	private void advance() {
		if (eventQueue.isEmpty()) {
			// no tokens are ready for the next round:
			// the simulation may be over, or may be blocked
			// waiting for an external event; signal the
			// parent clock that it can advance
			setQuiescent();
			return;
		} else {
			// Advance the clock to the next logical time and
			// resume all tokens waiting for that time
			LtimerQueueEntry top = eventQueue.peek();
			currentTime = top.time;
			while (top != null && top.time <= currentTime) {
				Token token = eventQueue.remove().token;
				token.unsetQuiescent();
				token.resume(currentTime);
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

		public LtimerQueueEntry(int time, Token token) {
			this.token = token;
			this.time = time;
		}
		public int compareTo(LtimerQueueEntry n) {
			// sort the queue items earliest first
			return Integer.signum(time - n.time);
		}
	}
	
	/**
	 * Add an active token. When moving a token between clocks,
	 * it is important to ensure that the token is removed after it is added.
	 */
	void addActive() {
		if (quiescent) unsetQuiescent();
		++active;
	}

	/**
	 * Remove an active token. When moving a token between clocks,
	 * it is important to ensure that the token is removed after it is added,
	 * so that the number of active tokens only hits zero once, at the end
	 * of a processing step.
	 */
	void removeActive() {
		assert(active > 0);
		--active;
		if (active == 0) advance();
	}
}