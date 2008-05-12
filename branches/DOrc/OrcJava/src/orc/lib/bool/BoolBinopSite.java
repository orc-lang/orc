/**
 * 
 */
package orc.lib.bool;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public abstract class BoolBinopSite extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		return new Constant(compute(args.boolArg(0), args.boolArg(1)));
	}

	abstract public boolean compute(boolean a, boolean b);
	
}
