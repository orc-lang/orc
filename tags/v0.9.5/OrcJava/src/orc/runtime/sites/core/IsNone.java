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
		if (None.data.deconstruct(args.getArg(0)) == null) {
			caller.die();
		} else {
			caller.resume(Value.signal());
		}
	}
}
