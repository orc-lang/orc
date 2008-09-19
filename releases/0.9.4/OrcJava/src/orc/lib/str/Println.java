/**
 * 
 */
package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;
import orc.type.EllipsisArrowType;
import orc.type.Type;

/**
 * @author dkitchin
 *
 * Print arguments, converted to strings, in sequence, each followed by newlines.
 *
 */
public class Println extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		for(int i = 0; i < args.size(); i++) {
			caller.print(String.valueOf(args.getArg(i)), true);
		}
		if (args.size() == 0) {
			caller.print("", true);
		}
		caller.resume(Value.signal());
	}
	
	public static Type type() {
		return new EllipsisArrowType(Type.STRING, Type.TOP);
	}

}
