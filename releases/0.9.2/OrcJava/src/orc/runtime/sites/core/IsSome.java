/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class IsSome extends Site {

	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		
		Value v = args.valArg(0);
		
		if (v.isSome()) {
			caller.resume(v.untag());
		}
		else {
			caller.die();
		}
	}

}
