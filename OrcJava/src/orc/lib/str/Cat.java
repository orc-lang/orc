/**
 * 
 */
package orc.lib.str;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * Note that you can also use the syntax "a" + "b" for string concatenation.
 * 
 * @author dkitchin
 */
public class Cat extends EvalSite {

	public Value evaluate(Args args) throws TokenException {
		
		StringBuffer buf = new StringBuffer();
		
		for(int i = 0; i < args.size(); i++)
		{
			buf.append(args.valArg(i).toString());
		}
		
		return new Constant(buf.toString());
	}
}
