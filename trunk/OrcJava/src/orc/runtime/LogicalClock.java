package orc.runtime;

import java.util.PriorityQueue;
import java.util.Queue;

import orc.runtime.values.Constant;
/**
 * 
 * An event queue for site calls to the logical timer (Ltimer).
 * The Orc engine advances the logical clock whenever there are no
 * other queued tokens to process.
 * 
 * Since the logical clock is advanced only by the Orc engine, and
 * calls to Ltimer are cooperatively executed by the same engine,
 * the logical clock object does not need to run in a separate thread,
 * and does not require synchronization.
 * 
 * @author dkitchin
 *
 */

public class LogicalClock {

	private Queue<LtimerQueueEntry> eventQueue;
	private long currentTime;
	
	public LogicalClock() {
		eventQueue = new PriorityQueue<LtimerQueueEntry>();
		currentTime = 0;
	}
	
	public long getTime() { return currentTime; }
	
	public void addEvent(long time, Token token) {
		eventQueue.add(new LtimerQueueEntry(time + currentTime, token));
	}
	
	/* Return true if advancing the clock queued one or more events, false otherwise */
	public boolean advance() {
		
		LtimerQueueEntry top = eventQueue.peek();
		
		/* If there is an entry in the event queue,
		 * resume the least entry, and all other entries
		 * with the same time.
		 * Advance the clock to that time.
		 */
		if (top != null) {
			currentTime = top.getTime();
			while(top != null && top.getTime() <= currentTime) {
				eventQueue.remove().getToken().resume(new Constant(currentTime));
				top = eventQueue.peek();
			}		
			return true;
		} 
		else {
			return false;
		}
		
	}
	
	/**
	 * Class representing Ltimer Queue Entry 
	 * @author dkitchin, based on RtimerQueueEntry by jayeshs
	 */
	class LtimerQueueEntry implements Comparable<LtimerQueueEntry> {

		long time;
		Token token;

		public LtimerQueueEntry(long time, Token token) {
			this.token = token;
			this.time = time;
		}
		public long getTime() {
			return time;
		}
		public Token getToken() {
			return token;
		}

		// sort the queue items earliest first
		public int compareTo(LtimerQueueEntry n) {
			long diff = time - n.time;

			if (diff == 0)
				return 0;
			else if (diff > 0)
				return 1;
			else
				return -1;
		}
	}	
	
}
