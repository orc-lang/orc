/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class If extends PartialSite implements PassedByValueSite {

	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		if (args.boolArg(0)) 
			return Value.signal();
		else
			return null;
	}

}
