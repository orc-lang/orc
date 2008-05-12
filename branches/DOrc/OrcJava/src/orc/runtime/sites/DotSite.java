/**
 * 
 */
package orc.runtime.sites;

import java.util.TreeMap;
import java.util.Map;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 * 
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using addMethods. The code is forward-compatible with many possible
 * optimizations on the field lookup strategy.
 * 
 * This cannot in general be passed by value because many sites keep state in
 * the top level. You can avoid this problem by using anonymous inner classes
 * for the member sites.
 */
public abstract class DotSite extends EvalSite {

	Map<String,Site> methodMap;
	
	public DotSite()
	{
		methodMap = new TreeMap<String,Site>();
		this.addMethods();
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		String f = args.fieldName();
		Site m = getMethod(f);
		
		if (m != null)
			{ return new orc.runtime.values.Site(m); }
		else
			{ throw new OrcRuntimeTypeException("Dotsite " + this.getClass().toString() + " does not have the method '" + f + "'."); }
	}
	
	Site getMethod(String f)
	{
		return methodMap.get(f);
	}

	// Subclasses implement this method with a sequence of addMethod calls.
	protected abstract void addMethods();	
	
	protected void addMethod(String f, Site s)
	{
		methodMap.put(f, s);
	}
	
}
