package orc.runtime.sites;

import java.util.concurrent.Callable;

import kilim.Pausable;
import kilim.Task;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Abstract class for sites whose calls may block (the Java thread).
 * @author quark
 */
public abstract class ThreadedSite extends Site {
	public void callSite(final Args args, Token caller) {
		Kilim.runThreaded(caller, new Callable<Object>() {
			public Object call() throws Exception {
				return evaluate(args);
			}
		});
	}
	abstract public Object evaluate(Args args) throws TokenException;
}
