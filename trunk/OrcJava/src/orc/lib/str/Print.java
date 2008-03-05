/**
 * 
 */
package orc.lib.str;

import orc.runtime.Args;
import orc.runtime.OrcRuntimeTypeError;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence.
 *
 */
public class Print extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeError {
		
		for(int i = 0; i < args.size(); i++)
		{
			System.out.print(args.stringArg(i));
		}
		
		return signal();
	}

}
