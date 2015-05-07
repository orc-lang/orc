package orc.runtime;

import java.util.PriorityQueue;
import java.util.Queue;
/**
 * 
 * An event queue for site calls to a logical timer (created by MakeTimer).
 * The Orc engine advances all logical clocks whenever there are no
 * other queued tokens to process.
 * 
 * Since logical clocks are advanced only by the Orc engine, and
 * calls to such timers are cooperatively executed by the same engine,
 * the logical clock objects do not need to run in a separate thread,
 * (unlike Rtimer), and they do not require synchronization.
 * 
 * @author dkitchin
 *
 */

public class LogicalClock {

	private Queue<LtimerQueueEntry> eventQueue;
	private int currentTime;
	
	public LogicalClock() {
		eventQueue = new PriorityQueue<LtimerQueueEntry>();
		currentTime = 0;
	}
	
	public int getTime() { return currentTime; }
	
	public void addEvent(int time, Token token) {
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
				eventQueue.remove().getToken().resume(currentTime);
				top = eventQueue.peek();
			}		
			return true;
		} else {
			return false;
		}
		
	}
	
	/* Return true if there are no logical timer events waiting, false otherwise */
	public boolean stuck() {
		return eventQueue.isEmpty();
	}
	
	/**
	 * Class representing Ltimer Queue Entry 
	 * @author dkitchin, based on RtimerQueueEntry by jayeshs
	 */
	class LtimerQueueEntry implements Comparable<LtimerQueueEntry> {

		int time;
		Token token;

		public LtimerQueueEntry(int time, Token token) {
			this.token = token;
			this.time = time;
		}
		public int getTime() {
			return time;
		}
		public Token getToken() {
			return token;
		}

		// sort the queue items earliest first
		public int compareTo(LtimerQueueEntry n) {
			int diff = time - n.time;

			if (diff == 0)
				return 0;
			else if (diff > 0)
				return 1;
			else
				return -1;
		}
	}	
	
}
