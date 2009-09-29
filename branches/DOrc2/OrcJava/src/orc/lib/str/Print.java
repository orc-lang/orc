/**
 * 
 */
package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.structured.EllipsisArrowType;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence.
 *
 */
public class Print extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			sb.append(String.valueOf(args.getArg(i)));
		}
		caller.print(sb.toString(), false);
		caller.resume(Value.signal());
	}
	
	public Type type() {
		return new EllipsisArrowType(Type.TOP, Type.TOP);
	}
}
