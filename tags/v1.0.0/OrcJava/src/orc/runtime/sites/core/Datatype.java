package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.ground.DatatypeSiteType;

/**
 * 
 * For each string argument, creates a datatype constructor site; the string is
 * used as a label for printing and debugging. Returns these sites as a tuple.
 * 
 * @author dkitchin
 *
 */
public class Datatype extends EvalSite {

	@Override
	public Object evaluate(Args args) throws TokenException {
		
		Object[] datasites = new Object[args.size()];
		
		for(int i = 0; i < datasites.length; i++) {
			
			String label = args.stringArg(i);
			datasites[i] = new Datasite(label);
		}
		return Let.condense(datasites);
	}

	public Type type() {
		return new DatatypeSiteType();
	}
	
}