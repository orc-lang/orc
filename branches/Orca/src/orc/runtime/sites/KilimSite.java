package orc.runtime.sites;

import kilim.Fiber;
import kilim.Pausable;
import kilim.Task;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.Kilim.PausableCallable;

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
	@Override
	public void callSite(final Args args, final Token caller)
	throws TokenException {
		Kilim.runPausable(caller, new PausableCallable<Object>() {
			public Object call() throws Pausable, TokenException {
				return evaluate(args);
			}
		});
	}
	
	public abstract Object evaluate(Args args) throws Pausable, TokenException;
	/** FIXME: Kilim should add this method but it doesn't */
	public Object evaluate(Args args, Fiber f) throws Pausable, TokenException {
		throw new AssertionError("Unwoven method "
				+ this.getClass().toString()
				+ "#call()");
		
	}
}
