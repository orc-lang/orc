package orc.orcx;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Sequence;


import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;


/**
 * 
 * Site object embedding an XQuery invocation.
 * 
 * Note that this is a slight deviation from how subclasses of Site typically behave:
 * an xquery invocation is strict in its arguments, but it may publish multiple values.
 * 
 * Semantically, this could be considered equivalent to returning a single sequence
 * value, and then invoking an 'explode' definition which traverses the sequence and
 * publishes each of its items.
 * 
 * @author dkitchin
 *
 */
public class OrcXSite extends Site {

	List<String> varnames;
	int queryid;
	
	public OrcXSite(int queryid, List<String> varnames) {
		this.queryid = queryid;
		this.varnames = varnames;
	}
	
	public void callSite(Args args, Token caller) {
		
		// Create a mapping of the variable names to the argument values
		Map<String, Value> argmap = new TreeMap<String, Value>();
		for(int i = 0; i < varnames.size(); i++) {
			argmap.put(varnames.get(i), args.valArg(i));
		}
		
		try {
		
		// Create a fresh dynamic context based on the static context and the argument list
		OrcXDynamicContext dynamicContext = new OrcXDynamicContext(staticContext, argmap);
		
		// Set this XQuery's context to the fresh context and evaluate it.
		// NOTE: This is not threadsafe, since we cannot clone the xquery. At the moment,
		// that's irrelevant, since the xquery call is cooperatively scheduled.
		xq.setContext(dynamicContext);
		
		
			Sequence response = xq.eval(Sequence.EMPTY_SEQUENCE);
			
			// Convert the response sequence to a list of values, and resume with a new
			// publication for each such value.
			for(Value v : OrcX.convertToValues(response)) {
				Token newProcess = caller.copy();
				newProcess.resume(v);
			}
			
		}
		catch (XPathException e) {
			e.printStackTrace();
			// Do nothing on an exception. The query remains silent forever.
		}
		catch (OrcXException e) {
			e.printStackTrace();
			// Do nothing on an exception. The query remains silent forever.
		}
		
	}

}
