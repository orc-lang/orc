package orc.orcx;

import java.util.Map;

import orc.runtime.values.Value;

import org.exist.dom.QName;
import org.exist.xquery.Variable;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;

/**
 * A dynamic execution context inherited from XQuery's context. 
 * In this subclass, we override resolveVariable to trap uses of global variables that
 * are not defined, and look up their bound Orc values.
 * 
 * @author dkitchin
 *
 */

public class OrcXDynamicContext extends XQueryContext { 
	
	private Map<String, Variable> orcvars;
	
	public OrcXDynamicContext(OrcXStaticContext ctx, Map<String, Value> orcvals) throws OrcXException {
		
		// Make an XQueryContext copy of the given static context
		super(ctx);
		
		// For each free var in the static context, find its Orc value mapping,
		// and create a variable with the corresponding XPath value.
		for(String var : ctx.getFreeVars()) {
			Value val = orcvals.get(var);
			Sequence seq = OrcX.convertToSequence(val);
			
			Variable v = new Variable(new QName(var));
			v.setValue(seq);
			orcvars.put(var, v);
		}
	}
	
	
	public Variable resolveVariable(QName qname) throws XPathException {
		
		Variable v = null;
		
		try {
			v = super.resolveVariable(qname); // see if this variable is already bound within the xquery
		}
		catch (XPathException e) { // if it's not defined, trap here and assume it's an Orc variable.
			try {
				String orcName = qname.getLocalName(); // get the local name as a string for Orc's use
				v = orcvars.get(orcName);
				if (v == null) { throw e; }  // if our mapping doesn't have this var, it's an error
			}
			catch (IllegalArgumentException iae) { // but we can only make it an Orc variable if it's a local name
				throw e; // if it's not a local name, reraise the original exception
			}
		}
		
		return v;
	}
	
}
