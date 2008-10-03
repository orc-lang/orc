package orc.runtime.sites.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

import orc.error.runtime.JavaException;
import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.MethodTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;


/**
 * @author dkitchin, quark
 */
public class ClassProxy extends EvalSite {
	private static final long serialVersionUID = 1L;
	/** Index methods by name. */
	private HashMap<String, MethodHandle> methods = new HashMap<String, MethodHandle>();
	private ConstructorHandle constructor;
	private Class type;
	
	private static class ConstructorHandle extends InvokableHandle<Constructor> {
		public ConstructorHandle(Constructor[] constructors) {
			super("<init>", constructors);
		}
		
		protected Class[] getParameterTypes(Constructor c) {
			return c.getParameterTypes();
		}
		
		protected int getModifiers(Constructor c) {
			return c.getModifiers();
		}
	}
	
	private static HashMap<Class, ClassProxy> cache = new HashMap<Class, ClassProxy>(); 
	
	/**
	 * These objects are cached so that instances of a class
	 * can share the same method handles.
	 */
	public static ClassProxy forClass(Class c) {
		ClassProxy out = cache.get(c);
		if (out == null) {
			out = new ClassProxy(c);
			cache.put(c, out);
		}
		return out;
	}

	private ClassProxy(Class c) {
		this.type = c;
		this.constructor = new ConstructorHandle(c.getConstructors());
	}

	@Override
	public Object evaluate(Args args) throws TokenException {
		// If this looks like a field reference, assume it is a reference to a static
		// method and treat it accordingly. That means you can't call a
		// constructor with a field as the first argument, which is OK since it
		// is impossible to create a bare field literal in Orc anyways.
		String member = null;
		try {
			member = args.fieldName(); 
		} catch (TokenException e) {
			// do nothing
		}
		if (member != null) {
			return getMember(null, member);
		}
		
		// Otherwise it's a constructor call
		Object[] argsArray = args.asArray();
		Constructor c = constructor.resolve(argsArray);
		try {
			return c.newInstance(argsArray);
		} catch (InvocationTargetException e) {
			throw new JavaException(e.getCause());
		} catch (Exception e) {
			throw new JavaException(e);
		}
	}
	
	/**
	 * Get the value of an object member (method or field). If a method
	 * and field have the same name, the method is prefered. Since in Java
	 * method values aren't first class we wrap these in a MethodProxy.
	 */
	public Object getMember(Object self, String name) throws MessageNotUnderstoodException {
		try {
			MethodHandle handle = getMethod(name);
			return new MethodProxy(self, handle);
		} catch (MessageNotUnderstoodException e) {
			try {
				Field field = getField(name);
				return field.get(self);
				// Errors accessing the field are ignored,
				// as if we never looked for the filed at all.
				// I'm not sure if this is the right approach.
			} catch (IllegalAccessException _) {
				throw e;
			} catch (NoSuchFieldException _) {
				throw e;
			}
		}
	}
	
	/**
	 * Look up a field by name.
	 * FIXME: should distinguish between static and non-static fields?
	 */
	public Field getField(String name) throws SecurityException, NoSuchFieldException {
		return type.getField(name);
	}
	
	/**
	 * Look up a method by name. Method handles are cached so that
	 * future lookups can go faster.
	 * FIXME: should distinguish between static and non-static methods?
	 */
	public MethodHandle getMethod(String methodName) throws MessageNotUnderstoodException {
		if (methods.containsKey(methodName)) {
			return methods.get(methodName);
		}

		// Why not use getMethod to find a method appropriate to the argument
		// types? Because getMethod ignores subtyping, so if the argument types
		// don't exactly match any of the methods, one is chosen at random. I
		// assume that's because Java expects method overloading to be resolved
		// at compile time based on static types.
		List<Method> matchingMethods = new LinkedList<Method>();
		for (Method m : type.getMethods()) {
			if (m.getName().equals(methodName)) {
				matchingMethods.add(m);
			}
		}

		if (matchingMethods.isEmpty()) {
			throw new MessageNotUnderstoodException(methodName);
		}

		MethodHandle out = new MethodHandle(
				methodName, matchingMethods.toArray(new Method[]{}));
		methods.put(methodName, out);
		return out;
	}
}