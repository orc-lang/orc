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
import orc.type.ArrowType;
import orc.type.ListType;
import orc.type.OptionType;
import orc.type.TupleType;
import orc.type.Type;
import orc.type.TypeVariable;

/**
 * @author dkitchin
 */
public class IsSome extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		TupleValue result = Some.data.deconstruct(args.getArg(0));
		if (result == null) {
			caller.die();
		} else {
			caller.resume(result.at(0));
		}
	}
	
	public Type type() throws TypeException { 	
		Type X = new TypeVariable(0);
		Type OptionX = (new OptionType()).instance(X);
		return new ArrowType(OptionX, X, 1); 
	}

}
