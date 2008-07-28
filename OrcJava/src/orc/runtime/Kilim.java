package orc.runtime;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.values.Value;

import kilim.ExitMsg;
import kilim.Fiber;
import kilim.Mailbox;
import kilim.PauseReason;
import kilim.Scheduler;
import kilim.Semaphore;
import kilim.Task;
import kilim.pausable;

/**
 * Utility methods for 
 * <a href="http://kilim.malhar.net/">Kilim</a>.
 * 
 * @author quark
 */
public class Kilim {
	static {
		Scheduler.setDefaultScheduler(new Scheduler(10));
	}
	
	/**
	 * Used for {@link #runThreaded(Callable)} threads, to prevent
	 * thread resource exhaustion.
	 */
	private static final BoundedThreadPool pool = new BoundedThreadPool(40);
	
	/**
	 * Bounded thread pool with a pausable execute method.
	 * 
	 * @author quark
	 */
	public static class BoundedThreadPool {
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
			executor = new ThreadPoolExecutor(0, size*2, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()) {
				@Override
				protected void afterExecute(Runnable r, Throwable t) {
					// TODO Auto-generated method stub
					super.afterExecute(r, t);
					semaphore.release();
				}
			};
		}
		public @pausable void execute(Runnable thread) {
			semaphore.acquire();
			executor.execute(thread);
		}
	}
	
	/**
	 * Kilim mailboxes can't accomodate null values, so this
	 * acts as a basic signal or unit value.
	 */
	public static final Object signal = new Object();
	
	private static class Box<V> {
		public V value;
	}
	
	/**
	 * Pausable computation which returns a value.
	 * FIXME: Kilim incorrectly adds methods to interfaces
	 */
	public abstract static class Pausable<V> {
		public abstract @pausable V call() throws Exception;
		/** FIXME: Kilim should add this method but it doesn't */
		public V call(Fiber f) throws Exception {
			throw new AssertionError("Unwoven method "
					+ this.getClass().toString()
					+ "#call()");
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
	public @pausable static <V> V runThreaded(final Callable<V> thunk)
	throws Exception {
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
		if (out == box) return ((Box<V>)out).value;
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
	public @pausable static void runThreaded(final Runnable thunk) {
		pool.execute(thunk);
	}
	

	/**
	 * Utility method to run a pausable computation which generates a value
	 * for a token.
	 * @param caller token to return the value to
	 * @param thunk computation returning a value
	 */
	public static void runPausable(final Token caller, final Pausable<Value> thunk) {
		// In order to deal with both regular and irregular exits,
		// we have to set up a monitor task which spawns a child
		// task, monitors its exit value, and then does something
		// with the token depending on the exit value.
		new Task() {
			public @pausable void execute() {
				// distinguished value which signals that a value
				// was returned normally
        		final Value[] normalExit = new Value[1];
        		// start evaluating the site
        		final Mailbox<ExitMsg> exit = new Mailbox<ExitMsg>();
        		Task task = new Task() {
        			public @pausable void execute() {
        				try {
	        				normalExit[0] = thunk.call();
	    					exit(normalExit);
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
        		Object result = exit.get().result;
    			if (result instanceof TokenException) {
    				// a token exception
    				caller.error((TokenException)result);
				} else if (result instanceof Throwable) {
    				// some other exception
    				caller.error(new JavaException((Throwable)result));
    			} else if (result == normalExit) {
    				// a normal value
    				caller.resume(normalExit[0]);
        		} else {
        			// any other value is irrelevant, it
        			// signifies a dead token
    				caller.die();
        		}
			}
		}.start();
	}
}
