/**
 * 
 */
package orc.lib.str;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class AtoB extends PartialSite {

	public Value evaluate(Args args) throws TokenException {
		
		String s = args.stringArg(0);
		
		if (s.equals("true")) {
			return new Constant(true);
		}
		else if (s.equals("false")) {
			return new Constant(false);
		}
		else {
			return null;
		}
	}

}
