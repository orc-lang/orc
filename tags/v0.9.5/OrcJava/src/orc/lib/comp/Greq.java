package orc.lib.comp;

/**
 * @author dkitchin
 *
 */
public class Greq extends NumericComparisonSite {

	/* (non-Javadoc)
	 * @see orc.lib.comp.IntCompareSite#compare(int, int)
	 */
	@Override
	public boolean compare(int a) {
		return a >= 0;
	}

}
