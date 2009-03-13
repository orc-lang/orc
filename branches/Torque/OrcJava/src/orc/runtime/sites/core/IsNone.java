/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;
import orc.type.ArrowType;
import orc.type.OptionType;
import orc.type.Type;
import orc.type.TypeVariable;

/**
 * @author dkitchin
 */
public class IsNone extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		if (None.data.deconstruct(args.getArg(0)) == null) {
			caller.die();
		} else {
			caller.resume(Value.signal());
		}
	}
	
	public Type type() throws TypeException { 	
		Type X = new TypeVariable(0);
		Type OptionX = (new OptionType()).instance(X);
		return new ArrowType(OptionX, Type.TOP, 1); 
	}
}
