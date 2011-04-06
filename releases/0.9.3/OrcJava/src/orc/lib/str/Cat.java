/**
 * 
 */
package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * Note that you can also use the syntax "a" + "b" for string concatenation.
 * 
 * @author dkitchin
 */
public class Cat extends EvalSite {

	public Object evaluate(Args args) throws TokenException {
		
		StringBuffer buf = new StringBuffer();
		
		for(int i = 0; i < args.size(); i++)
		{
			buf.append(String.valueOf(args.getArg(i)));
		}
		
		return buf.toString();
	}
}