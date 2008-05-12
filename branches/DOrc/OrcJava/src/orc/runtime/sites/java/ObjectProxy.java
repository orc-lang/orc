/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;

/**
 * Should this site be passed by value or not? We could leave this
 * up to the object being wrapped, but while most Java objects are
 * mutable and should typically be passed by reference, they do
 * not automatically perform the necessary RMI stuff. So we'll
 * go the safe route and assume they must be shared.
 * 
 * @author dkitchin
 */
public class ObjectProxy extends EvalSite {
	private static final long serialVersionUID = 1L;

	Object wrapped_instance;

	public ObjectProxy(Object inst) {
		this.wrapped_instance = inst;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[],
	 *      orc.runtime.Token, orc.runtime.values.GroupCell,
	 *      orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		return new orc.runtime.values.Site(getMethodProxy(args));
	}
	
	protected MethodProxy getMethodProxy(Args args) throws OrcRuntimeTypeException {
		String methodName = args.fieldName();
		List<Method> matching_methods = new LinkedList<Method>();

		// Why not use getMethod to find a method appropriate to the argument
		// types? Because getMethod ignores subtyping, so if the argument types
		// don't exactly match any of the methods, one is chosen at random. I
		// assume that's because Java expects method overloading to be resolved
		// at compile time based on static types.
		for (Method m : wrapped_instance.getClass().getMethods()) {
			if (m.getName().equals(methodName)) {
				matching_methods.add(m);
			}
		}

		if (matching_methods.isEmpty()) {
			throw new OrcRuntimeTypeException("Class "
					+ wrapped_instance.getClass().toString()
					+ " does not have the method '" + methodName + "'.");
		}

		return new MethodProxy(matching_methods, wrapped_instance, methodName);	
	}
}