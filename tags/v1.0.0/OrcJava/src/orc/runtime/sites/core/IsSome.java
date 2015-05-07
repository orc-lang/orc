/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.ListType;
import orc.type.structured.OptionType;
import orc.type.structured.TupleType;

/**
 * @author dkitchin
 */
public class IsSome extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		Object result = Some.data.deconstruct(args.getArg(0));
		if (result == null) {
			caller.die();
		} else {
			caller.resume(result);
		}
	}
	
	public Type type() throws TypeException { 	
		Type X = new TypeVariable(0);
		Type OptionX = (new OptionType()).instance(X);
		return new ArrowType(OptionX, X, 1); 
	}

}
