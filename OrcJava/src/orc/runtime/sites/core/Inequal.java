/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class Inequal extends EvalSite {

	public Value evaluate(Args args) throws TokenException {
		
		Object a = args.getArg(0);
		Object b = args.getArg(1);
		return new Constant(!a.equals(b));
		
	}

}
