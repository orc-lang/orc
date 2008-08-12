/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 */
public class IsNone extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		Object v = args.getArg(0);
		if (v instanceof Value && ((Value)v).isNone()) {
			caller.resume(Value.signal());
		} else {
			caller.die();
		}
	}
}
