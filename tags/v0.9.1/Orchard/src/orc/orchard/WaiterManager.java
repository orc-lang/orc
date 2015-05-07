package orc.orchard;

import java.util.LinkedList;

/**
 * Manage waiters waiting for the same event. This is NOT thread-safe -- since
 * you should only call it from within a synchronized block there is no need.
 * 
 * @author quark
 */
public class WaiterManager {
	private LinkedList<Waiter> queue = new LinkedList<Waiter>();
	private Object monitor;
	
	public WaiterManager(Object monitor) {
		this.monitor = monitor;
	}
	
	public void suspend(Waiter w) throws InterruptedException {
		queue.remove(w);
		queue.add(w);
		w.suspend(monitor);
	}
	public void resume() {
		if (queue.isEmpty()) return;
		queue.removeFirst().resume();
	}
	public void resumeAll() {
		while (!queue.isEmpty()) {
			queue.removeFirst().resume();
		}
	}
}