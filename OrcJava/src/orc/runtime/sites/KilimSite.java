package orc.runtime.sites;

import kilim.ExitMsg;
import kilim.Fiber;
import kilim.Mailbox;
import kilim.Scheduler;
import kilim.Task;
import kilim.pausable;
import orc.error.JavaException;
import orc.error.SiteException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Use <a href="http://kilim.malhar.net/">Kilim</a> to run a site with
 * lightweight threads and no direct access to tokens.
 * 
 * <p>The site's evaluate method is run in a Kilim {@link Task}. If the task
 * calls {@link kilim.Task#exit(Object)}, the token is killed; if the
 * task throws an exception, the token is killed with an error. Otherwise
 * the token uses the return value.
 * 
 * @author quark
 * 
 */
public abstract class KilimSite extends Site {
	static {
		// FIXME: the scheduler should use a thread-local variable
		// for the default, so that each Orc engine can have its own scheduler
		Scheduler.setDefaultScheduler(new Scheduler(1));
	}
	
	/**
	 * Pausible computation which returns a value.
	 * FIXME: Kilim bugs out when I make the return type generic.
	 * FIXME: Kilim bugs out on interfaces
	 * FIXME: Kilim bugs out on abstract methods
	 */
	public static class Callable {
		public @pausable Value call() throws Exception {
			throw new AssertionError("Method not woven");
		}
	}
	
	/**
	 * Utility method to run a pausable computation which generates a value
	 * for a token.
	 * @param caller token to return the value to
	 * @param thunk computation returning a value
	 */
	public static void runPausable(final Token caller, final Callable thunk) {
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
        			public @pausable void execute() throws Exception {
        				normalExit[0] = thunk.call();
    					exit(normalExit);
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

	@Override
	public void callSite(final Args args, final Token caller)
	throws TokenException {
		runPausable(caller, new Callable() {
			public @pausable Value call() throws TokenException {
				return evaluate(args);
			}
		});
	}

	@pausable
	public Value evaluate(Args args) throws TokenException {
		throw new SiteException("You must override KilimSite#evaluate(Args)");
	}
}
