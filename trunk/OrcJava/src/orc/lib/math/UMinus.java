/**
 * 
 */
package orc.lib.math;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public class UMinus extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		return -intArg(args, 0);
	}

}
