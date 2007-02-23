/**
 * 
 */
package orc.runtime.sites;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;

/**
 * @author dkitchin
 *
 * 0: the site that doesn't return.
 *
 */
public class Zero extends Site {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public void callSite(Object[] args, Token returnToken, GroupCell caller,
			OrcEngine engine) {
		// Do nothing. The calling token is removed from the engine and never reactivated.
	}

}
