/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 */
public class IsSome extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		TupleValue result = Some.data.deconstruct(args.getArg(0));
		if (result == null) {
			caller.die();
		} else {
			caller.resume(result.at(0));
		}
	}
}
