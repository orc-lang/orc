/**
 * 
 */
package orc.lib.str;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence.
 *
 */
public class Print extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		for(int i = 0; i < args.size(); i++) {
			caller.getEngine().print(args.stringArg(i));
		}
		caller.resume(Value.signal());
	}
}
