package orc.lib.util;

import kilim.Pausable;
import kilim.Task;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;

/**
 * Wrap a site call in a (pooled) thread. This is useful if you have a Java
 * site which may be uncooperative. If the site really must have its own
 * thread, use {@link UnsafeThreadSite} instead.
 * 
 * @author quark
 */
public class ThreadSite extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		Site thunk;
		try {
			thunk = (Site)args.getArg(0);
		} catch (ClassCastException e) {
			throw new JavaException(e);
		}
		return makeThreaded(thunk);
	}
	public static Site makeThreaded(final Site site) {
		return new Site() {
			public void callSite(final Args args, final Token caller) {
				// Use Kilim.runThreaded to run this in Kilim's
				// thread pool. The task doesn't do anything but
				// wait for a thread to become available
				new Task() {
					public void execute() throws Pausable {
						Kilim.runThreaded(new Runnable() {
							public void run() {
								try {
									site.callSite(args, caller);
								} catch (TokenException e) {
									caller.error(e);
								}
							}
						});
					}
				}.start();
			}
		};
	}
}
