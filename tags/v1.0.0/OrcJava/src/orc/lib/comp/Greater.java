/**
 * 
 */
package orc.lib.comp;

/**
 * @author dkitchin
 *
 */
public class Greater extends ComparisonSite {
	@Override
	public boolean compare(int a) {
		return a > 0;
	}
}
