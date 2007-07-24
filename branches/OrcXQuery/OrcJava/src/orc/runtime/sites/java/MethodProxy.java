/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Method;
import java.util.List;

import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * @author dkitchin
 *
 */
public class MethodProxy extends EvalSite {

	List<Method> wrapped_methods;
	Object self;
	String methodName;
	
	public MethodProxy(List<Method> m, Object self, String name)
	{
		this.wrapped_methods = m;
		this.self = self;
		this.methodName = name;
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
	 */
	@Override
	public Value evaluate(Args args) {
		
		Object result = null;
		
		for (Method m : wrapped_methods)
		{
			try
			{
				result = m.invoke(self, args.asArray());
				break;
			}
			catch (IllegalArgumentException e) {}
			catch (Exception e) { throw new Error("Method invocation failure for '" + methodName + "'."); }
		}
		
		if (result == null)
			{ throw new Error("Argument types did not match any implementation for method '" + methodName + "'."); }

		return new Constant(result);
	}

}
