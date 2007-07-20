/**
 * 
 */
package orc.lib.math;

import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public abstract class IntBinopSite extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Tuple args) {
		
		return new Constant(compute(args.intArg(0), args.intArg(1)));
	}

	abstract public int compute(int a, int b);
	
}
