/**
 * 
 */
package orc.lib.bool;

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
	public Object evaluate(Object[] args) {
		
		return compute(boolArg(args,0), boolArg(args,1));
	}

	abstract public boolean compute(boolean a, boolean b);
	
}
