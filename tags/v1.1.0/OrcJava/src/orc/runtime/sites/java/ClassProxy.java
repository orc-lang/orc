//
// ClassProxy.java -- Java class ClassProxy
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.sites.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import orc.error.runtime.CapabilityException;
import orc.error.runtime.JavaException;
import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.java.ConstructorType;

/**
 * @author dkitchin, quark
 */
public class ClassProxy extends Site {
	/** Index methods by name. */
	private final HashMap<String, MethodHandle> methods = new HashMap<String, MethodHandle>();
	private final ConstructorHandle constructor;
	private final Class type;

	private static HashMap<Class, ClassProxy> cache = new HashMap<Class, ClassProxy>();

	/**
	 * These objects are cached so that instances of a class
	 * can share the same method handles.
	 */
	public static ClassProxy forClass(final Class c) {
		ClassProxy out = cache.get(c);
		if (out == null) {
			out = new ClassProxy(c);
			cache.put(c, out);
		}
		return out;
	}

	private ClassProxy(final Class c) {
		this.type = c;
		this.constructor = new ConstructorHandle(c.getConstructors());
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		// If this looks like a field reference, assume it is a reference to a static
		// method and treat it accordingly. That means you can't call a
		// constructor with a field as the first argument, which is OK since it
		// is impossible to create a bare field literal in Orc anyways.
		String member = null;
		try {
			member = args.fieldName();
		} catch (final TokenException e) {
			// do nothing
		}
		if (member != null) {
			if (member.equals("?")) {
				caller.resume(new MatchProxy(type));
			} else {
				caller.resume(getMember(caller, null, member));
			}
			return;
		}

		// Otherwise it's a constructor call
		final Object[] argsArray = args.asArray();
		final Constructor c = constructor.resolve(argsArray);

		// FIXME: too much indirection is necessary to run this in a site thread
		Kilim.runThreaded(caller, new Callable<Object>() {
			public Object call() throws Exception {
				try {
					return c.newInstance(argsArray);
				} catch (final InvocationTargetException e) {
					throw new JavaException(e.getCause());
				}
			}
		});
	}

	/**
	 * Get the value of an object member (method or field). If a method
	 * and field have the same name, the method is prefered. Since in Java
	 * method values aren't first class we wrap these in a MethodProxy.
	 * @throws SecurityException 
	 * @throws CapabilityException 
	 */
	public Object getMember(final Token token, final Object self, final String name) throws MessageNotUnderstoodException, CapabilityException, SecurityException {
		try {
			final MethodHandle handle = getMethod(token, name);
			return new MethodProxy(self, handle);
		} catch (final MessageNotUnderstoodException e) {
			try {
				final Field field = getField(token, name);
				return new FieldProxy(self, field);
				// Errors accessing the field are ignored,
				// as if we never looked for the field at all.
				// I'm not sure if this is the right approach.
			} catch (final NoSuchFieldException _) {
				throw e;
			}
		}
	}

	/**
	 * Look up a field by name.
	 * FIXME: should distinguish between static and non-static fields?
	 * @throws CapabilityException 
	 */
	public Field getField(final Token token, final String name) throws SecurityException, NoSuchFieldException, CapabilityException {
		if (name.equals("getClass")) {
			token.requireCapability("import java", true);
		}
		return type.getField(name);
	}

	/**
	 * Look up a method by name. Method handles are cached so that
	 * future lookups can go faster.
	 * FIXME: should distinguish between static and non-static methods?
	 * @throws CapabilityException 
	 */
	public MethodHandle getMethod(final Token token, final String methodName) throws MessageNotUnderstoodException, CapabilityException {
		if (methodName.equals("getClass")) {
			token.requireCapability("import java", true);
		}
		if (methods.containsKey(methodName)) {
			return methods.get(methodName);
		}

		// Why not use getMethod to find a method appropriate to the argument
		// types? Because getMethod ignores subtyping, so if the argument types
		// don't exactly match any of the methods, one is chosen at random. I
		// assume that's because Java expects method overloading to be resolved
		// at compile time based on static types.
		final List<Method> matchingMethods = new LinkedList<Method>();
		for (final Method m : type.getMethods()) {
			if (m.getName().equals(methodName)) {
				matchingMethods.add(m);
			}
		}

		if (matchingMethods.isEmpty()) {
			throw new MessageNotUnderstoodException(methodName);
		}

		final MethodHandle out = new MethodHandle(methodName, matchingMethods.toArray(new Method[] {}));
		methods.put(methodName, out);
		return out;
	}

	@Override
	public Type type() {
		return new ConstructorType(type);
	}
}
