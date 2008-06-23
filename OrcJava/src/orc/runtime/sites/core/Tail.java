/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class Tail extends EvalSite {


	public Value evaluate(Args args) throws TokenException {
				
		return args.valArg(0).tail();
	}

}
