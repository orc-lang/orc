/**
 * 
 */
package orc.lib.bool;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public abstract class BoolBinopSite extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Args args) throws TokenException {
		
		return compute(args.boolArg(0), args.boolArg(1));
	}

	abstract public boolean compute(boolean a, boolean b);
	
}
