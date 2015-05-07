/**
 * 
 */
package orc.runtime.sites.core;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.NoneValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class IsCons extends EvalSite {


	public Value evaluate(Args args) throws TokenException {
		
		Value v = args.valArg(0);
		
		if (v.isCons()) 
			return new SomeValue(new TupleValue(v.head(), v.tail()));
		else
			return new NoneValue();
	}

}
