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
public class IsSome extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		Object v = args.getArg(0);
		if (v instanceof Value && ((Value)v).isSome()) {
			caller.resume(((Value)v).untag());
		} else {
			caller.die();
		}
	}
}
