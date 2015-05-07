/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.NoneValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class IsNil extends EvalSite {

	@Override
	public Value evaluate(Args args) throws TokenException {

		Value v = args.valArg(0);
		
		if (v.isNil()) {
			return new SomeValue(v);
		}
		else {
			return new NoneValue();
		}	

	}

}
