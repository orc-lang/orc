package orc.runtime;

import java.util.concurrent.Callable;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.sites.KilimSite.PausableCallable;
import orc.runtime.values.Value;

import kilim.ExitMsg;
import kilim.Mailbox;
import kilim.Task;
import kilim.pausable;

public class Kilim {
	/**
	 * Utility method to run a threaded computation which generates a value.
	 * This is a common idiom in a Kilim computation.
	 * 
	 * <p>FIXME: I ran into about 3 different weird Kilim bugs trying
	 * to write this function; the current structure is not ideal but
	 * it was the only one which worked.
	 */
	public @pausable static <V> V runThreaded(final Callable<V> thunk)
	throws Exception {
		// local class to indicate a successful return
		class Ok { public V value; }
		final Mailbox<Object> mbox = new Mailbox<Object>();
		new Thread() {
			public void run() {
				try {
					Ok out = new Ok();
					out.value = thunk.call();
    				mbox.putb(out);
				} catch (Exception e) {
    				mbox.putb(e);
				}
			}
		}.start();
		Object out = mbox.get();
		if (out instanceof Ok) return ((Ok)out).value;
		else throw (Exception)out;
	}
	

	/**
	 * Utility method to run a pausable computation which generates a value
	 * for a token.
	 * @param caller token to return the value to
	 * @param thunk computation returning a value
	 */
	public static void runPausable(final Token caller, final PausableCallable<Value> thunk) {
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
