package orc.runtime.sites.java;

import orc.error.TokenException;
import orc.lib.util.ThreadSite;
import orc.runtime.Args;
import orc.runtime.Token;

/**
 * Objects whose methods should always be called in new threads.
 * This is to be avoided, but it's safer than allowing native
 * methods to block the interpreter.
 * @author quark
 */
public class ThreadedObjectProxy extends ObjectProxy {
	public ThreadedObjectProxy(Object inst) {
		super(inst);
	}
	public void callSite(final Args args, final Token caller) throws TokenException {
		String methodName;
		try {
			methodName = args.fieldName();
		} catch (TokenException e) {
			// If this looks like a site call, call the special method "apply".
			new ThreadedMethodProxy(getDelegate("apply")).callSite(args, caller);
			return;
		}
		caller.resume(ThreadSite.makeThreaded(new MethodProxy(getDelegate(methodName))));
	}
}
