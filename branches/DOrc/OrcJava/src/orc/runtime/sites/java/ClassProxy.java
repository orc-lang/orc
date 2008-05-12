/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;



/**
 * Site providing access to a Java class (static methods and constructors).
 * This cannot in general be passed by value because static members may be
 * mutated.
 * @author dkitchin
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
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		Object inst = null;
		
		// If this looks like a field reference, assume it is a call to a static
		// method and treat it accordingly. That means you can't call a
		// constructor with a field as the first argument, which is OK since it
		// would be hard to create a bare field literal in Orc anyways.
		// FIXME: explicitly preserve the distinction between field reference
		// and site call so we don't need this hack.
		String methodName = null;
		try {
			methodName = args.fieldName(); 
		} catch (OrcRuntimeTypeException e) {
			// do nothing
		}
		if (methodName != null) {
			List<Method> matching_methods = new LinkedList<Method>();
			for (Method m : wrapped_class.getMethods()) {
				if (m.getName().equals(methodName)) {
					matching_methods.add(m);
				}
			}

			if (matching_methods.isEmpty()) {
				throw new OrcRuntimeTypeException("Class "
						+ wrapped_class.toString()
						+ " does not have the method '" + methodName + "'.");
			}

			return new orc.runtime.values.Site(new MethodProxy(matching_methods, null, methodName));
		}
		
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

