/**
 * 
 */
package orc.lib.comp;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public abstract class IntCompareSite extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		
		return compare(intArg(args,0), intArg(args,1));
	}

	abstract public boolean compare(int a, int b);
	
}
