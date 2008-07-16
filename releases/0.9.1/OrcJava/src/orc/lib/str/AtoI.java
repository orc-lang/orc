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
public class AtoI extends PartialSite {

	public Value evaluate(Args args) throws TokenException {
		
		String s = args.stringArg(0);
		
		try {
			return new Constant(Integer.parseInt(s));
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
