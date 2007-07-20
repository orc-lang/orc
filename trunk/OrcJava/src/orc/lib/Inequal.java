/**
 * 
 */
package orc.lib;

import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Tuple;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class Inequal extends EvalSite {

	public Value evaluate(Tuple args) {
		
		Object a = args.getArg(0);
		Object b = args.getArg(1);
		return new Constant(!a.equals(b));
		
	}

}
