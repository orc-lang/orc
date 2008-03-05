/**
 * 
 */
package orc.lib.comp;

import orc.runtime.Args;
import orc.runtime.OrcRuntimeTypeError;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public abstract class IntCompareSite extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeError{
		
		return new Constant(compare(args.intArg(0), args.intArg(1)));
	}

	abstract public boolean compare(int a, int b);
	
}
