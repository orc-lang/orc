/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class Not extends EvalSite {
	
	@Override
	public Value evaluate(Args args) throws TokenException {
		return new Constant(!args.boolArg(0));
	}

}
