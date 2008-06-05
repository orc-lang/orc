package orc.runtime.sites;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Abstract class for sites whose calls may block (the Java thread).
 * A separate thread is created for every call.
 * @author quark
 */
public abstract class ThreadedSite extends Site {
	public void callSite(final Args args, final Token caller) throws OrcRuntimeTypeException {
		new Thread() {
			public void run() {
				try {
					caller.resume(evaluate(args));
				} catch (OrcRuntimeTypeException e) {
					caller.error(e);
				}
			}
		}.start();
	}
	abstract public Value evaluate(Args args) throws OrcRuntimeTypeException;
}
