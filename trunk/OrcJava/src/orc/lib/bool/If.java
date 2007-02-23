/**
 * 
 */
package orc.lib.bool;

import orc.runtime.sites.PartialSite;

/**
 * @author dkitchin
 *
 */
public class If extends PartialSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.PartialSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		
		if (boolArg(args,0)) 
			return true;
		else
			return null;
	}

}
