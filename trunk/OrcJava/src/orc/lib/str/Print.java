/**
 * 
 */
package orc.lib.str;

import orc.runtime.sites.EvalSite;
import orc.runtime.values.Tuple;
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
	public Value evaluate(Tuple args) {
		
		for(int i = 0; i < args.size(); i++)
		{
			System.out.print(args.stringArg(i));
		}
		
		return signal();
	}

}
