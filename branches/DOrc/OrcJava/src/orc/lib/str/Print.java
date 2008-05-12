/**
 * 
 */
package orc.lib.str;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 * 
 * Print arguments, converted to strings, in sequence.
 */
public class Print extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		for(int i = 0; i < args.size(); i++)
		{
			System.out.print(args.stringArg(i));
		}
		
		return Value.signal();
	}

}
