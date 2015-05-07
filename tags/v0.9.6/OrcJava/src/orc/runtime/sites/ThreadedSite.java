package orc.runtime.sites;

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
	public void callSite(final Args args, final Token caller) {
		new Task() {
			public void execute() throws Pausable {
				Kilim.runThreaded(new Runnable() {
					public void run() {
						try {
							Object out = evaluate(args);
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
	abstract public Object evaluate(Args args) throws TokenException;
}
