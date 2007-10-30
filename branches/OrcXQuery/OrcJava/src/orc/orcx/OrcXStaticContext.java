package orc.orcx;

import java.util.LinkedList;
import java.util.List;

import org.exist.dom.QName;
import org.exist.security.xacml.AccessContext;
import org.exist.storage.DBBroker;
import org.exist.xquery.Variable;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;

/**
 * A static context inherited from XQuery's context. 
 * In this subclass, we override resolveVariable to trap uses of global variables that
 * are not defined, and record them for later use as Orc variables.
 * 
 * @author dkitchin
 *
 */

public class OrcXStaticContext extends XQueryContext {

	private List<String> orcvars; 
	
	public OrcXStaticContext(DBBroker broker) {
		super(broker, AccessContext.INTERNAL_PREFIX_LOOKUP);
		
		orcvars = new LinkedList<String>();
	}

	public List<String> getFreeVars() {
		return orcvars;
	}
	
	public Variable resolveVariable(QName qname) throws XPathException {
		
		Variable v = null;
		
		try {
			v = super.resolveVariable(qname); // see if this variable is already bound within the xquery
		}
		catch (XPathException e) { // if it's not defined, trap here and assume it's an Orc variable.
			try { 
				String orcName = qname.getLocalName(); // get the local name as a string for Orc's use
				orcvars.add(orcName);
				
				v = new Variable(qname);
			}
			catch (IllegalArgumentException iae) { // but we can only make it an Orc variable if it's a local name
				throw e; // if it's not a local name, reraise the original exception
			}
		}
		
		return v;
	}

}
