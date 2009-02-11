/**
 * 
 */
package orc.runtime.sites.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.ListLike;
import orc.runtime.values.Value;
import orc.type.ArrowType;
import orc.type.ListType;
import orc.type.TupleType;
import orc.type.Type;
import orc.type.TypeVariable;

/**
 * @author dkitchin
 *
 */
public class TryCons extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		args.listLikeArg(0).uncons(caller);
	}
	
	public Type type() throws TypeException { 	
		Type X = new TypeVariable(0);
		Type ListOfX = (new ListType()).instance(X);
		return new ArrowType(ListOfX, new TupleType(X, ListOfX), 1); 
	}
	
}
