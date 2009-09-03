/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.ListType;
import orc.type.structured.TupleType;

/**
 * @author dkitchin
 *
 */
public class TryNil extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		args.listLikeArg(0).unnil(caller);
	}
	
	public Type type() throws TypeException { 	
		Type ListOfTop = (new ListType()).instance(Type.TOP);
		return new ArrowType(ListOfTop, Type.TOP, 1); 
	}
}