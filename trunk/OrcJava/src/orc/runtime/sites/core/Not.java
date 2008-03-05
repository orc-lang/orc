/**
 * 
 */
package orc.runtime.sites.core;

import orc.runtime.Args;
import orc.runtime.OrcRuntimeTypeError;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class Not extends EvalSite {
	
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeError {
		return new Constant(!args.boolArg(0));
	}

}
