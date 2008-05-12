/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class Inequal extends EvalSite implements PassedByValueSite {

	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		Object a = args.getArg(0);
		Object b = args.getArg(1);
		return new Constant(!a.equals(b));
		
	}

}
