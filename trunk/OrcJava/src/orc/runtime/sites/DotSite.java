/**
 * 
 */
package orc.runtime.sites;

import java.util.TreeMap;
import java.util.Map;

import orc.error.MessageNotUnderstoodException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 * 
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using addMethods. The code is forward-compatible with many possible
 * optimizations on the field lookup strategy.
 */
public abstract class DotSite extends EvalSite {

	Map<String,Value> methodMap;
	
	public DotSite()
	{
		methodMap = new TreeMap<String,Value>();
		this.addMethods();
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) throws TokenException {
		
		String f = args.fieldName();
		Value m = getMethod(f);
		
		if (m != null)
			{ return m; }
		else
			{ throw new MessageNotUnderstoodException(f); } 
	}
	
	Value getMethod(String f) {
		return methodMap.get(f);
	}

	// Subclasses implement this method with a sequence of addMethod calls.
	protected abstract void addMethods();	
	
	protected void addMethod(String f, Value s) {
		methodMap.put(f, s);
	}
	
}
