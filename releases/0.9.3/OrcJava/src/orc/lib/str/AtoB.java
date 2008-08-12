/**
 * 
 */
package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;

/**
 * @author dkitchin
 *
 */
public class AtoB extends PartialSite {
	public Object evaluate(Args args) throws TokenException {
		String s = args.stringArg(0);
		
		if (s.equals("true")) {
			return true;
		} else if (s.equals("false")) {
			return false;
		} else {
			return null;
		}
	}
}
