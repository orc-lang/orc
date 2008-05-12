/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class Tail extends EvalSite implements PassedByValueSite {


	public Value evaluate(Args args) throws OrcRuntimeTypeException {
				
		return args.valArg(0).tail();
	}

}
