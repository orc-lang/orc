package orc.runtime.sites;

import kilim.Task;
import kilim.pausable;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Abstract class for partial sites whose calls may block (the Java thread). A
 * separate thread is created for every call.
 * 
 * @author quark
 */
public abstract class ThreadedPartialSite extends Site {
	public void callSite(final Args args, final Token caller) {
		new Task() {
			public @pausable void execute() {
				Kilim.runThreaded(new Runnable() {
					public void run() {
						try {
							caller.resume(evaluate(args));
						} catch (TokenException e) {
							caller.error(e);
						}
					}
				});
			}
		}.start();
	}

	abstract public Value evaluate(Args args) throws TokenException;
}
