package orc.runtime.sites;

import java.util.concurrent.Callable;

import kilim.ExitMsg;
import kilim.Mailbox;
import kilim.Scheduler;
import kilim.Task;
import kilim.pausable;
import orc.error.JavaException;
import orc.error.SiteException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
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
 * <p>Killing a token will not automatically exit the corresponding task,
 * because we don't know what kind of cleanup may be necessary and tasks
 * don't provide a good way to define such.
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
	public static class PausableCallable<V> {
		public @pausable V call() throws Exception {
			throw new AssertionError("Method not woven");
		}
	}


	@Override
	public void callSite(final Args args, final Token caller)
	throws TokenException {
		Kilim.runPausable(caller, new PausableCallable() {
			public @pausable Object call() throws TokenException {
				return evaluate(args);
			}
		});
	}

	@pausable
	public Value evaluate(Args args) throws TokenException {
		throw new SiteException("You must override KilimSite#evaluate(Args)");
	}
}
