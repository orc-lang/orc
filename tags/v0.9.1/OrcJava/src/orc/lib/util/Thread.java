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
 * site which may be uncooperative.
 * @author quark
 */
public class Thread extends EvalSite {
	@Override
	public Value evaluate(Args args) throws TokenException {
		final Site thunk;
		try {
			thunk = (Site)args.valArg(0);
		} catch (ClassCastException e) {
			throw new JavaException(e);
		}
		return new Site() {
			public void callSite(final Args args, final Token caller) {
				new java.lang.Thread() {
					public void run() {
						try {
							thunk.callSite(args, caller);
						} catch (TokenException e) {
							caller.error(e);
						}
					}
				}.start();
			}
		};
	}
}
