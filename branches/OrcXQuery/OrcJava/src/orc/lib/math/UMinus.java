/**
 * 
 */
package orc.lib.math;

import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class UMinus extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) {
		return new Constant(-args.intArg(0));
	}
	
}
