/**
 * 
 */
package orc.lib.str;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 * 
 * Print arguments, converted to strings, in sequence, each followed by
 * newlines.
 */
public class Println extends EvalSite implements PassedByValueSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		for(int i = 0; i < args.size(); i++)
		{
			System.out.println(args.stringArg(i));
		}
		
		if (args.size() == 0) { System.out.println(); }
		
		return Value.signal();
	}

}
