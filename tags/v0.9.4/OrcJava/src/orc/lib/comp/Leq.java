/**
 * 
 */
package orc.lib.comp;

/**
 * @author dkitchin
 *
 */
public class Leq extends NumericComparisonSite {

	/* (non-Javadoc)
	 * @see orc.lib.comp.IntCompareSite#compare(int, int)
	 */
	@Override
	public boolean compare(int a, int b) {
		return a <= b;
	}

}
