/**
 * 
 */
package orc.lib.bool;

import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class Not extends EvalSite {
	
	@Override
	public Value evaluate(Args args) {
		return new Constant(!args.boolArg(0));
	}

}
