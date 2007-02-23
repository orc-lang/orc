/**
 * 
 */
package orc.runtime.sites;

/**
 * @author dkitchin
 *
 */
public class Signal extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		return signal();
	}

}
