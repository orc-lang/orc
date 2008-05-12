/**
 * 
 */
package orc.lib.math;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public abstract class IntBinopSite extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		return new Constant(compute(args.intArg(0), args.intArg(1)));
	}

	abstract public int compute(int a, int b);
	
}
