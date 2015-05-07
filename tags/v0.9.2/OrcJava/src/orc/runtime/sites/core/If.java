/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class If extends PartialSite {

	@Override
	public Value evaluate(Args args) throws TokenException {
		if (args.boolArg(0)) 
			return Value.signal();
		else
			return null;
	}

}
