package orc.runtime.sites;

import java.util.concurrent.Callable;

import kilim.Task;
import kilim.pausable;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.Kilim.Pausable;
import orc.runtime.values.Value;

/**
 * Abstract class for sites whose calls may block (the Java thread).
 * @author quark
 */
public abstract class ThreadedSite extends Site {
	public void callSite(final Args args, final Token caller) {
		new Task() {
			public @pausable void execute() {
				Kilim.runThreaded(new Runnable() {
					public void run() {
						try {
							Value out = evaluate(args);
							if (out == null) caller.die();
							else caller.resume(out);
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
