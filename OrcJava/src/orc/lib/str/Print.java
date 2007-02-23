/**
 * 
 */
package orc.lib.str;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence.
 *
 */
public class Print extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		
		for (Object x : args)
		System.out.print(x.toString());
		
		// TODO: There should be an explicit 'signal' value
		return null;
	}

}
