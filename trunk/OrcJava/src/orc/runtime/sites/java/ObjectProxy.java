/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import orc.runtime.sites.EvalSite;


/**
 * @author dkitchin
 *
 */
public class ObjectProxy extends EvalSite {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Object wrapped_instance;
	
	public ObjectProxy(Object inst) {
		this.wrapped_instance = inst;
	}
	
	public Object unbox() {
		return wrapped_instance;
	}
	
	public Object asBasicValue()
	{
		return wrapped_instance;
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Object[] args) {

		String methodName = (String)getArg(args, 0);
		List<Method> matching_methods = new LinkedList<Method>();
		
		for (Method m : wrapped_instance.getClass().getMethods())
		{
			if (m.getName().equals(methodName))
			{
				matching_methods.add(m);
			}
		}
		
		if (matching_methods.isEmpty())
			{ throw new Error("Class " + wrapped_instance.getClass().toString() + " does not have the method '" + methodName + "'."); }

		return new MethodProxy(matching_methods, wrapped_instance, methodName);
	}

}
