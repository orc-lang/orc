/**
 * 
 */
package orc.runtime.sites.java;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public class Unbox extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {

		ObjectProxy p = (ObjectProxy)(getArg(args,0));
		
		return p.unbox();
	}

}
