/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Constructor;

import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;



/**
 * @author dkitchin
 *
 */
public class ClassProxy extends EvalSite {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Class wrapped_class;
	
	public ClassProxy(Class c)
	{
		this.wrapped_class = c;
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Tuple args) {

				Object inst = null;
				
				// Attempt to construct an instance of the class, using the site call args as parameters
				// Try each of the possible constructors until one matches.
				for (Constructor cn : wrapped_class.getConstructors())
				{
					try
					{
						inst = cn.newInstance(args.asArray());
					}
					catch (IllegalArgumentException e) {}
					catch (Exception e) { throw new Error("Error creating instance of " + wrapped_class.toString()); }
				}
				
				// It's possible that none of the constructors worked; this is a runtime error.
				if (inst == null)	 	
				{	
					throw new Error("Proxy constructor call failed for " + wrapped_class.toString()); 
				}
	
				// create a proxy for the object
				return new Constant(inst);
				 
		
	}

}

