/**
 * 
 */
package orc.lib.math;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public abstract class IntBinopSite extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		
		return compute(intArg(args,0), intArg(args,1));
	}

	abstract public int compute(int a, int b);
	
}
