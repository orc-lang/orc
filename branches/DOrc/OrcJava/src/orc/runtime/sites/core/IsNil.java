/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.NoneValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class IsNil extends EvalSite implements PassedByValueSite {

	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {

		Value v = args.valArg(0);
		
		if (v.isNil()) {
			return new SomeValue(v);
		}
		else {
			return new NoneValue();
		}	

	}

}
