/**
 * 
 */
package orc.lib;

import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class Equal extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) {
		
		Object a = args.getArg(0);
		Object b = args.getArg(1);
		return new Constant(a.equals(b));
		
	}

}
