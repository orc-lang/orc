package orc.lib.util;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * Wrap a site call in a thread. This is useful if you have a Java
 * site which may be uncooperative. It's also unsafe, since you
 * can easily run out of threads. {@link ThreadSite} is better since
 * it uses a thread pool, but that can cause deadlock if you have
 * dependencies between threads.
 * 
 * @author quark
 */
public class UnsafeThreadSite extends EvalSite {
	@Override
	public Value evaluate(Args args) throws TokenException {
		Site thunk;
		try {
			thunk = (Site)args.valArg(0);
		} catch (ClassCastException e) {
			throw new JavaException(e);
		}
		return makeThreaded(thunk);
	}
	public static Site makeThreaded(final Site site) {
		return new Site() {
			public void callSite(final Args args, final Token caller) {
				new java.lang.Thread() {
					public void run() {
						try {
							site.callSite(args, caller);
						} catch (TokenException e) {
							caller.error(e);
						}
					}
				}.start();
			}
		};
	}
}
