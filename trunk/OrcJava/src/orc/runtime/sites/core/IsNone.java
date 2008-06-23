/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class IsNone extends Site {

	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		
		Value v = args.valArg(0);
		
		if (v.isNone()) {
			caller.resume(Value.signal());
		}
		else {
			caller.die();
		}
	}

}
