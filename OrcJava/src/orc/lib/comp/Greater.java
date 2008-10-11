/**
 * 
 */
package orc.lib.comp;

/**
 * @author dkitchin
 *
 */
public class Greater extends NumericComparisonSite {
	@Override
	public boolean compare(int a) {
		return a > 0;
	}
}
