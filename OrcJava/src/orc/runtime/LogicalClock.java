package orc.runtime;

import java.util.PriorityQueue;

/**
 * An event queue for site calls to a logical timer (created by MakeTimer).
 * 
 * <p>
 * According to the semantics of logical time, logical timers can only advance
 * when there are no pending site calls except those to logical timers. However
 * it's not so clear how logical timers should interact with pull and semicolon.
 * For the moment they are allowed to advance even if there are tokens blocked
 * on a group cell or semicolon.
 * 
 * <p>
 * Since logical clocks are advanced only by the Orc engine, and calls to such
 * timers are cooperatively executed by the same engine, the logical clock
 * objects do not need to run in a separate thread, (unlike Rtimer), and they do
 * not require synchronization.
 * 
 * @author dkitchin
 * 
 */

public class LogicalClock {

	private PriorityQueue<LtimerQueueEntry> eventQueue;
	private int currentTime = 0;
	
	public LogicalClock() {
		eventQueue = new PriorityQueue<LtimerQueueEntry>();
		currentTime = 0;
	}
	
	public int getTime() { return currentTime; }
	
	public void addEvent(int time, Token token) {
		eventQueue.add(new LtimerQueueEntry(time + currentTime, token));
		token.unsetPending();
	}
	
	/* Return true if advancing the clock queued one or more events, false otherwise */
	public boolean advance() {
		// no tokens are ready for the next round
		if (eventQueue.isEmpty()) return false;
		
		// Advance the clock to the next logical time and
		// resume all tokens waiting for that time
		LtimerQueueEntry top = eventQueue.peek();
		currentTime = top.time;
		while (top != null && top.time <= currentTime) {
			Token token = eventQueue.remove().token;
			token.setPending();
			token.resume(currentTime);
			top = eventQueue.peek();
		}		
		return true;
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
}
