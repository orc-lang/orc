/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.runtime.values.Value;
import orc.type.ArrowType;
import orc.type.Type;

/**
 * @author dkitchin
 *
 */
public class If extends PartialSite {

	@Override
	public Object evaluate(Args args) throws TokenException {
		if (args.boolArg(0)) 
			return Value.signal();
		else
			return null;
	}
	
	public static Type type() {
		return new ArrowType(Type.BOOLEAN, Type.TOP);
	}

}
