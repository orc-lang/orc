/**
 * 
 */
package orc.lib;

import orc.runtime.sites.EvalSite;

/**
 * @author dkitchin
 *
 */
public class Equal extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Object evaluate(Object[] args) {
		
		// typecase would be lovely here!
		if (args[0] instanceof Boolean && args[1] instanceof Boolean)
			return boolArg(args, 0) == boolArg(args, 1);

		if (args[0] instanceof Integer && args[1] instanceof Integer)
			return intArg(args, 0) == intArg(args, 1);

		if (args[0] instanceof String && args[1] instanceof String)
			return stringArg(args, 0).equals(stringArg(args,1));
		
		return args[0] == args[1];
	}

}
