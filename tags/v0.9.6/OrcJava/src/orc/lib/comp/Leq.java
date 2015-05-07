/**
 * 
 */
package orc.lib.comp;

/**
 * @author dkitchin
 *
 */
public class Leq extends ComparisonSite {

	/* (non-Javadoc)
	 * @see orc.lib.comp.IntCompareSite#compare(int, int)
	 */
	@Override
	public boolean compare(int a) {
		return a <= 0;
	}

}
