package orc.orchard;

/**
 * Use Java wait/notify to implement suspend/resume.
 * @see Waiter
 * @author quark
 */
public class ThreadWaiter implements Waiter {
	private Object monitor;
	public void suspend(Object monitor) throws InterruptedException {
		this.monitor = monitor;
		this.monitor.wait();
	}
	public void resume() {
		this.monitor.notify();
	}
}
