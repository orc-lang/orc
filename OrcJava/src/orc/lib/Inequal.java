/**
 * 
 */
package orc.lib;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 * Just wraps Equal and negates it.
 *
 */
public class Inequal extends EvalSite {

	static Equal e = new Equal();
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		return !((Boolean)e.evaluate(args));
	}

}
