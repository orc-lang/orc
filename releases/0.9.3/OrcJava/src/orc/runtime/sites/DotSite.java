/**
 * 
 */
package orc.runtime.sites;

import java.util.Map;
import java.util.TreeMap;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;

/**
 * @author dkitchin
 * 
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using addMethods. The code is forward-compatible with many possible
 * optimizations on the field lookup strategy.
 */
public abstract class DotSite extends EvalSite {

	Map<String,Object> methodMap;
	
	public DotSite()
	{
		methodMap = new TreeMap<String,Object>();
		this.addMethods();
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) throws TokenException {
		
		String f = args.fieldName();
		Object m = getMethod(f);
		
		if (m != null)
			{ return m; }
		else
			{ throw new MessageNotUnderstoodException(f); } 
	}
	
	Object getMethod(String f) {
		return methodMap.get(f);
	}

	// Subclasses implement this method with a sequence of addMethod calls.
	protected abstract void addMethods();	
	
	protected void addMethod(String f, Object s) {
		methodMap.put(f, s);
	}
	
}
