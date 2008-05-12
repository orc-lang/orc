package orc.runtime;

import java.rmi.RemoteException;
import java.util.PriorityQueue;
import java.util.Queue;

import orc.runtime.values.Constant;
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
	
	public void addEvent(int time, RemoteToken caller) {
		eventQueue.add(new LtimerQueueEntry(time + currentTime, caller));
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
				try {
					eventQueue.remove().getToken().resume(new Constant(currentTime));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					throw new RuntimeException(e);
				}
				top = eventQueue.peek();
			}		
			return true;
		} 
		else {
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
		RemoteToken token;

		public LtimerQueueEntry(int time, RemoteToken token) {
			this.token = token;
			this.time = time;
		}
		public int getTime() {
			return time;
		}
		public RemoteToken getToken() {
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
