package orc.runtime.sites.core;


import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

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
		
		List<Object> datasites = new LinkedList<Object>();
		
		for(int i = 0; i < args.size(); i++) {
			
			String label = args.stringArg(i);
			datasites.add(new Datasite(label));
		}
		return new TupleValue(datasites);
	}

}
