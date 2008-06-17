package orc.orchard;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Manage waiters waiting for the same event.
 * This is NOT thread-safe!
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
		try {
			queue.removeFirst().resume(monitor);
		} catch (NoSuchElementException e) {
			return;
		}
	}
}