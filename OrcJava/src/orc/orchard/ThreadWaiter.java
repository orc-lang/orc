package orc.orchard;

/**
 * Use Java wait/notify to implement suspend/resume.
 * @see Waiter
 * @author quark
 */
public class ThreadWaiter implements Waiter {
	public void suspend(Object monitor) throws InterruptedException {
		monitor.wait();
	}
	public void resume(Object monitor) {
		monitor.notify();
	}
}
