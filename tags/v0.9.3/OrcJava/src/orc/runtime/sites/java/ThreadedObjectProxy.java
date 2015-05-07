package orc.runtime.sites.java;

import orc.error.runtime.TokenException;
import orc.lib.util.ThreadSite;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.sites.java.ObjectProxy.DelegateCache;

/**
 * Objects whose methods should always be called in new threads.
 * This is to be avoided, but it's safer than allowing native
 * methods to block the interpreter.
 * @author quark
 */
public class ThreadedObjectProxy extends Site {
	private DelegateCache delegates;
	public ThreadedObjectProxy(Object inst) {
		delegates = new DelegateCache(inst.getClass(), inst);
	}
	public void callSite(final Args args, final Token caller) throws TokenException {
		String methodName;
		try {
			methodName = args.fieldName();
		} catch (TokenException e) {
			// If this looks like a site call, call the special method "apply".
			ThreadSite.makeThreaded(new MethodProxy(delegates.get("apply")))
				.callSite(args, caller);
			return;
		}
		caller.resume(ThreadSite.makeThreaded(new MethodProxy(delegates.get(methodName))));
	}
}
