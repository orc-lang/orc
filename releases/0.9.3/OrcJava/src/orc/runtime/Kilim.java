package orc.runtime;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kilim.ExitMsg;
import kilim.Fiber;
import kilim.Mailbox;
import kilim.Pausable;
import kilim.Scheduler;
import kilim.Semaphore;
import kilim.Task;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;

/**
 * Utility methods for 
 * <a href="http://kilim.malhar.net/">Kilim</a>.
 * 
 * @author quark
 */
public class Kilim {
	/**
	 * The scheduler for the current Orc engine.
	 * The box is necessary because values are "inherited" by
	 * copying, and we need to "inherit" the value before it
	 * is actually set.
	 */
	static InheritableThreadLocal<Box<Scheduler>> scheduler =
		new InheritableThreadLocal<Box<Scheduler>>();
	/**
	 * Delegate to the current scheduler.
	 */
	private static class DynamicScheduler extends Scheduler {
		public DynamicScheduler() {
			super(0);
		}
		@Override
		public synchronized void schedule(Task task) {
			Scheduler current = scheduler.get().value;
			// Setting the scheduler is necessary to ensure
			// that this task can be resumed from different
			// threads.  It's also more efficient.
			task.setScheduler(current);
			current.schedule(task);
		}
		@Override
		public synchronized void shutdown() {
			scheduler.get().value.shutdown();
		}
		@Override
		public synchronized void dump() {
			scheduler.get().value.dump();
		}
	}
	static {
		Scheduler.setDefaultScheduler(new DynamicScheduler());
	}
	
	/**
	 * Bounded thread pool with a pausable execute method.
	 * 
	 * @author quark
	 */
	private static class BoundedThreadPool {
		private Semaphore semaphore;
		private ThreadPoolExecutor executor;
		public BoundedThreadPool(int size) {
			semaphore = new Semaphore(size);
			// FIXME: in theory, we should only need "size"
			// threads at most. In practice, afterExecute
			// runs before the thread becomes available,
			// so there's a window where we may need an extra
			// thread or two. Worst case, if every thread stops
			// at once, we'll need twice as many threads.
			executor = new ThreadPoolExecutor(0, size*2, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()) {
				@Override
				protected void afterExecute(Runnable r, Throwable t) {
					// TODO Auto-generated method stub
					super.afterExecute(r, t);
					semaphore.release();
				}
			};
		}
		public void execute(Runnable thread) throws Pausable {
			semaphore.acquire();
			executor.execute(thread);
		}
		public void shutdownNow() {
			executor.shutdownNow();
		}
	}
	
	/**
	 * Used for {@link #runThreaded(Callable)} threads, to prevent
	 * thread resource exhaustion. This uses a box for the same
	 * reason as {@link #scheduler}.
	 */
	static InheritableThreadLocal<Box<BoundedThreadPool>> pool =
		new InheritableThreadLocal<Box<BoundedThreadPool>>();
	
	/**
	 * Initialize Kilim state for a new job.
	 */
	static void startEngine(int kilimThreads, int siteThreads) {
		Box<Scheduler> _scheduler = new Box<Scheduler>();
		Box<BoundedThreadPool> _pool = new Box<BoundedThreadPool>();
		scheduler.set(_scheduler);
		pool.set(_pool);
		// both of these will start a bunch of threads
		// when we construct them; using a box allows
		// those threads to see the very objects which
		// created them
		_scheduler.value = new Scheduler(kilimThreads);
		_pool.value = new BoundedThreadPool(siteThreads);
	}
	
	/**
	 * Shutdown Kilim threads created for the current job.
	 */
	static void stopEngine() {
		scheduler.get().value.shutdown();
		pool.get().value.shutdownNow();
		scheduler.set(null);
		pool.set(null);
	}
	
	/**
	 * Kilim mailboxes can't accomodate null values, so this
	 * acts as a basic signal or unit value when necessary.
	 */
	public static final Object signal = new Object();
	
	private static class Box<V> {
		public V value;
	}
	
	/**
	 * Pausable computation which returns a value.
	 * FIXME: Kilim incorrectly adds methods to interfaces
	 */
	public static abstract class PausableCallable<V> {
		public abstract V call() throws Pausable, Exception;
		/** FIXME: Kilim should add this method but it doesn't */
		public V call(Fiber f) throws Exception {
			throw new AssertionError("Unwoven method "
					+ this.getClass().toString()
					+ "#call()");
		}
	}
	
	/**
	 * Wrap a callable in a lazy, concurrency-safe promise.
	 * FIXME: if the callable exits, waiters will NOT be notified.
	 * @author quark
	 */
	public static class Promise<V> extends PausableCallable<V> { 
		private enum State { NEW, FORCING, READY };
		private State state = State.NEW;
		private V value;
		private final List<Mailbox> waiters = new LinkedList<Mailbox>();
		private final PausableCallable<V> thunk;
		public Promise(PausableCallable<V> thunk) {
			this.thunk = thunk;
		}
		public synchronized V call() throws Pausable, Exception {
			switch (state) {
			case FORCING: 
				Mailbox<V> inbox = new Mailbox<V>();
				waiters.add(inbox);
				return inbox.get();
			case NEW: 
				state = State.FORCING;
				value = thunk.call();
				for (Mailbox<V> outbox : waiters) {
					outbox.put(value);
				}
			default: 
				return value;
			}
		}
	}
	
	/**
	 * Spawn a thread to compute a value.
	 * This is a common idiom in a Kilim computation, when you need
	 * to make blocking calls.
	 * 
	 * <p>FIXME: I ran into about 5 different weird Kilim bugs trying
	 * to write this function; the current structure is not ideal but
	 * it was the only one which worked.
	 * 
	 * <p>FIXME: Kilim mailboxes cannot handle null messages so we
	 * always have to return some object.
	 */
	public static <V> V runThreaded(final Callable<V> thunk)
	throws Pausable, Exception {
		final Box<V> box = new Box<V>();
		final Mailbox<Object> mbox = new Mailbox<Object>();
		runThreaded(new Runnable() {
			public void run() {
				try {
					box.value = thunk.call();
					mbox.putb(box);
				} catch (Exception e) {
					mbox.putb(e);
				}
			}
		});
		Object out = mbox.get();
		if (out == box) return box.value;
		else throw (Exception)out;
	}
	
	/**
	 * Spawn a thread asynchronously. If you want to block until
	 * the thread is complete, use {@link #runThreaded(Callable)}.
	 * The point of this over {@code new Thread() { ... }.start()} is
	 * that this uses a thread pool.
	 * 
	 * @param thunk
	 */
	public static void runThreaded(final Runnable thunk) throws Pausable {
		pool.get().value.execute(thunk);
	}

	/**
	 * Utility method to run a pausable computation which generates a value
	 * for a token.
	 * @param caller token to return the value to
	 * @param thunk computation returning a value
	 */
	public static void runPausable(final Token caller, final PausableCallable<? extends Object> thunk) {
		// In order to deal with both regular and irregular exits,
		// we have to set up a monitor task which spawns a child
		// task, monitors its exit value, and then does something
		// with the token depending on the exit value.
		new Task() {
			public void execute() throws Pausable {
				// distinguished value which signals that a value
				// was returned normally
				final Box<Object> box = new Box<Object>();
        		// start evaluating the site
        		final Mailbox<ExitMsg> exit = new Mailbox<ExitMsg>();
        		Task task = new Task() {
        			public void execute() throws Pausable {
        				try {
        					box.value = thunk.call();
	    					exit(box);
        				} catch (Exception e) {
        					// if we just throw the exception,
        					// Kilim will print an unwanted
        					// error message
        					exit(e);
        				}
        			}
        		};
        		task.informOnExit(exit);
        		task.start();
        		// wait for the site to finish
        		Object out = exit.get().result;
				if (out == box) {
					caller.resume(box.value);
				} else if (out instanceof TokenException) {
    				// a token exception
    				caller.error((TokenException)out);
				} else if (out instanceof Throwable) {
    				// some other exception
    				caller.error(new JavaException((Throwable)out));
        		} else {
        			// any other value is irrelevant, it
        			// signifies a dead token
    				caller.die();
        		}
			}
		}.start();
	}
}
