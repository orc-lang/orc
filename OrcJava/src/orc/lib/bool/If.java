/**
 * 
 */
package orc.lib.bool;

import orc.runtime.sites.PartialSite;
import orc.runtime.values.Tuple;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class If extends PartialSite {

	@Override
	public Value evaluate(Tuple args) {
		if (args.boolArg(0)) 
			return signal();
		else
			return null;
	}

}
