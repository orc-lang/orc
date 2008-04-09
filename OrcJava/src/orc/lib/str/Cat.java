/**
 * 
 */
package orc.lib.str;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class Cat extends EvalSite {

	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		StringBuffer buf = new StringBuffer();
		
		for(int i = 0; i < args.size(); i++)
		{
			buf.append(args.stringArg(i));
		}
		
		return new Constant(buf.toString());
	}

}
