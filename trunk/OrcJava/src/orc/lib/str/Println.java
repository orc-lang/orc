/**
 * 
 */
package orc.lib.str;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence, each followed by newlines.
 *
 */
public class Println extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		for(int i = 0; i < args.size(); i++) {
			caller.getEngine().println(args.stringArg(i));
		}
		if (args.size() == 0) {
			caller.getEngine().println("");
		}
		caller.resume(Value.signal());
	}
}
