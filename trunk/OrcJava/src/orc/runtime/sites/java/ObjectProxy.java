/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.error.MessageNotUnderstoodException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * A Java object being used as an Orc Site. This allows you to get a reference
 * to methods on the object using dot notation (like a DotSite).
 * 
 * <p>Methods are assumed to be non-blocking (although they may use {@link kilim}
 * for cooperative threading if desired). For objects with blocking methods,
 * use ThreadedObjectProxy.
 * 
 * @author dkitchin
 */
public class ObjectProxy extends Site {
	private static final long serialVersionUID = 1L;
	
	public static class Delegate {
		public String name;
		public Method[] methods;
		public Object that;
		public Delegate(String name, Method[] methods, Object that) {
			this.name = name;
			this.methods = methods;
			this.that = that;
		}
	}

	Object wrapped;
	Map<String, Delegate> methods = new HashMap<String, Delegate>();

	public ObjectProxy(Object inst) {
		this.wrapped = inst;
	}
	
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		String methodName;
		try {
			methodName = args.fieldName();
		} catch (TokenException e) {
			// If this looks like a site call, call the special method "apply".
			new MethodProxy(getDelegate("apply")).callSite(args, caller);
			return;
		}
		caller.resume(new MethodProxy(getDelegate(methodName)));
	}
	
	protected Delegate getDelegate(String methodName) throws TokenException {
		if (methods.containsKey(methodName)) {
			return methods.get(methodName);
		}

		// Why not use getMethod to find a method appropriate to the argument
		// types? Because getMethod ignores subtyping, so if the argument types
		// don't exactly match any of the methods, one is chosen at random. I
		// assume that's because Java expects method overloading to be resolved
		// at compile time based on static types.
		List<Method> matchingMethods = new LinkedList<Method>();
		for (Method m : wrapped.getClass().getMethods()) {
			if (m.getName().equals(methodName)) {
				matchingMethods.add(m);
			}
		}

		if (matchingMethods.isEmpty()) {
			/* throw new TokenException("Class "
					+ wrapped.getClass().toString()
					+ " does not have the method '" + methodName + "'."); */
			throw new MessageNotUnderstoodException(methodName);
		}

		Delegate out = new Delegate(methodName, matchingMethods.toArray(new Method[]{}), wrapped);
		methods.put(methodName, out);
		return out;
	}
}