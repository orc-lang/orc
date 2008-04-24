/**
 * 
 */
package orc.lib.bool;

/**
 * @author dkitchin
 *
 */
public class And extends BoolBinopSite {

	/* (non-Javadoc)
	 * @see orc.lib.bool.BoolBinopSite#compute(boolean, boolean)
	 */
	@Override
	public boolean compute(boolean a, boolean b) {
		return a && b;
	}

}
