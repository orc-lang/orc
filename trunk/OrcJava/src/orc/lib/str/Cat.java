/**
 * 
 */
package orc.lib.str;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public class Cat extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		
		StringBuffer buf = new StringBuffer();
		for (Object x : args)
			buf.append(x.toString());
		return buf.toString();
	}

}
