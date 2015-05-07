package orc.runtime.sites;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.Args;
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
	public void callSite(Args args, Token caller) throws TokenException {
		caller.getEngine().setCurrentToken(caller);
		Value value = evaluate(args);
		// If evaluation throws an exception, either before or after calling
		// suspend, it will be handled externally by killing the token. If the
		// site managed to save the token before throwing the exception, that's
		// ok, the token won't allow itself to be processed if it is ever
		// resumed.
		caller = caller.getEngine().getCurrentToken();
		if (caller != null) {
			// the site never suspended, so we can go ahead
			// and resume with the value it returned
			caller.resume(value);
		}
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