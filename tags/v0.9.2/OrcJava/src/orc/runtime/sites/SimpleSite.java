package orc.runtime.sites;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Continuation;
import orc.runtime.Continuation.Thunk;
import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Simple sites which don't need access to tokens. Cooperative threading is
 * provided via orc.runtime.Continuation. This class makes it easy to implement
 * variations on EvalSite, ThreadedSite, PartialSite, and combinations thereof.
 * 
 * @author quark
 */
public abstract class SimpleSite extends Site {
	@Override
	public void callSite(final Args args, Token caller) throws TokenException {
		Continuation.withToken(caller, new Thunk() {
			public Value apply() throws TokenException {
				return evaluate(args);
			}
		});
	}
	/**
	 * If this method calls Continuation.suspend(), its return value will be
	 * ignored and the engine will expect the method to resume or kill the
	 * continuation when ready. Otherwise, the site is assumed to be total and
	 * immediate and the return value will be used as the published value of the
	 * call.
	 */
	public abstract Value evaluate(Args args) throws TokenException;
}