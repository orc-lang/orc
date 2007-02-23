/**
 * 
 */
package orc.lib.bool;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public class Not extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		return !boolArg(args,0);
	}

}
