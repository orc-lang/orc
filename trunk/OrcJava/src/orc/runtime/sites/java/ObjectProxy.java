/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import orc.error.MessageNotUnderstoodException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.SimpleSite;
import orc.runtime.values.*;

/**
 * A Java object being used as an Orc Site. This allows you to get a reference
 * to methods on the object using dot notation (like a DotSite).
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
	public Value evaluate(Args args) throws TokenException {
		String methodName;
		try {
			methodName = args.fieldName();
		} catch (TokenException e) {
			// If this looks like a site call, call the special method "apply".
			return getMethodProxy("apply").evaluate(args);
		}
		return getMethodProxy(methodName);
	}
	
	protected MethodProxy getMethodProxy(String methodName) throws TokenException {
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
			/* throw new TokenException("Class "
					+ wrapped_instance.getClass().toString()
					+ " does not have the method '" + methodName + "'."); */
			throw new MessageNotUnderstoodException(methodName);
		}

		return new MethodProxy(matching_methods, wrapped_instance, methodName);	
	}
}