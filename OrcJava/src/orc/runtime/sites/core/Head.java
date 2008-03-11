/**
 * 
 */
package orc.runtime.sites.core;

import orc.runtime.Args;
import orc.runtime.OrcRuntimeTypeError;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class Head extends EvalSite {


	public Value evaluate(Args args) throws OrcRuntimeTypeError {
				
		return args.valArg(0).head();
	}

}
