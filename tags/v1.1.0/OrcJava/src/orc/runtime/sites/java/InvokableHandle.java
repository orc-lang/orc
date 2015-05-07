//
// InvokableHandle.java -- Java class InvokableHandle
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import orc.error.runtime.InsufficientArgsException;
import orc.error.runtime.MethodTypeMismatchException;

/**
 * Share common functionality of {@link MethodHandle} and
 * {@link ConstructorHandle}.
 * 
 * @author quark
 */
public abstract class InvokableHandle<M> {
	private static int arity(final Object[] things) {
		return Math.min(10, things.length);
	}

	public static enum Type {
		Object, Double, Float, Character, Boolean, Long, Integer, Short, Byte, LongObject, IntegerObject, ShortObject, ByteObject, DoubleObject, FloatObject, CharacterObject, BooleanObject, BigInteger, BigDecimal,
	};

	private final static HashMap<Class, Type> CLASS_TYPES;
	private final static boolean COERCABLE[][];

	private static void allowCoercion(final Type from, final Type to) {
		COERCABLE[from.ordinal()][to.ordinal()] = true;
	}

	static {
		COERCABLE = new boolean[Type.values().length][Type.values().length];

		// Standard numeric unboxing and widening
		allowCoercion(Type.DoubleObject, Type.Double);
		allowCoercion(Type.FloatObject, Type.Float);
		allowCoercion(Type.FloatObject, Type.Double);
		allowCoercion(Type.LongObject, Type.Long);
		allowCoercion(Type.IntegerObject, Type.Integer);
		allowCoercion(Type.IntegerObject, Type.Long);
		allowCoercion(Type.ShortObject, Type.Short);
		allowCoercion(Type.ShortObject, Type.Integer);
		allowCoercion(Type.ShortObject, Type.Long);
		allowCoercion(Type.ByteObject, Type.Byte);
		allowCoercion(Type.ByteObject, Type.Short);
		allowCoercion(Type.ByteObject, Type.Integer);
		allowCoercion(Type.ByteObject, Type.Long);
		allowCoercion(Type.CharacterObject, Type.Character);
		allowCoercion(Type.BooleanObject, Type.Boolean);

		// FIXME: without implicit narrowing, it's much more
		// awkward to use BigInteger and BigDecimal by default,
		// but this is semantically questionable
		allowCoercion(Type.BigDecimal, Type.Double);
		allowCoercion(Type.BigDecimal, Type.Float);
		allowCoercion(Type.BigInteger, Type.Double);
		allowCoercion(Type.BigInteger, Type.Float);
		allowCoercion(Type.BigInteger, Type.Long);
		allowCoercion(Type.BigInteger, Type.Integer);
		allowCoercion(Type.BigInteger, Type.Short);
		allowCoercion(Type.BigInteger, Type.Byte);

		CLASS_TYPES = new HashMap<Class, Type>();
		// primitive types
		CLASS_TYPES.put(Double.TYPE, Type.Double);
		CLASS_TYPES.put(Float.TYPE, Type.Float);
		CLASS_TYPES.put(Long.TYPE, Type.Long);
		CLASS_TYPES.put(Integer.TYPE, Type.Integer);
		CLASS_TYPES.put(Short.TYPE, Type.Short);
		CLASS_TYPES.put(Byte.TYPE, Type.Byte);
		CLASS_TYPES.put(Character.TYPE, Type.Character);
		CLASS_TYPES.put(Boolean.TYPE, Type.Boolean);
		// wrapper types
		CLASS_TYPES.put(Double.class, Type.DoubleObject);
		CLASS_TYPES.put(Float.class, Type.FloatObject);
		CLASS_TYPES.put(Long.class, Type.LongObject);
		CLASS_TYPES.put(Integer.class, Type.IntegerObject);
		CLASS_TYPES.put(Short.class, Type.ShortObject);
		CLASS_TYPES.put(Byte.class, Type.ByteObject);
		CLASS_TYPES.put(Character.class, Type.CharacterObject);
		CLASS_TYPES.put(Boolean.class, Type.BooleanObject);
		// big numbers
		CLASS_TYPES.put(BigInteger.class, Type.BigInteger);
		CLASS_TYPES.put(BigDecimal.class, Type.BigDecimal);
	}

	public static Type classToType(final Class type) {
		final Type out = CLASS_TYPES.get(type);
		return out == null ? Type.Object : out;
	}

	private static boolean resolve(final Class[] parameterTypes, final Object[] arguments) {
		// skip methods with the wrong number of arguments
		// FIXME: support varargs
		// FIXME: use Java's overload resolution rules to pick most specific
		// rather than just the first match
		if (parameterTypes.length != arguments.length) {
			return false;
		}
		// check argument types
		for (int i = 0; i < parameterTypes.length; ++i) {
			if (!isCoercableFrom(parameterTypes[i], arguments[i])) {
				return false;
			}
		}
		return true;
	}

	private static void coerce(final Class[] parameterTypes, final Object[] arguments) {
		// perform implicit coercions if necessary
		for (int i = 0; i < parameterTypes.length; ++i) {
			arguments[i] = coerce(parameterTypes[i], arguments[i]);
		}
	}

	/**
	 * Perform any necessary implicit coercion of an Orc object to a Java type.
	 */
	@SuppressWarnings("incomplete-switch")
	public static Object coerce(final Class to, final Object o) {
		if (o == null) {
			return null;
		} else if (o instanceof BigInteger) {
			final BigInteger b = (BigInteger) o;
			switch (classToType(to)) {
			case Double:
				return b.doubleValue();
			case Float:
				return b.floatValue();
			case Long:
				return b.longValue();
			case Integer:
				return b.intValue();
			case Short:
				return b.shortValue();
			case Byte:
				return b.byteValue();
			}
		} else if (o instanceof BigDecimal) {
			final BigDecimal b = (BigDecimal) o;
			switch (classToType(to)) {
			case Double:
				return b.doubleValue();
			case Float:
				return b.floatValue();
			}
		}
		// other coercions are handled by auto-unboxing
		return o;
	}

	/**
	 * Check whether an Orc object can be coerced to a Java class.
	 */
	public static boolean isCoercableFrom(final Class to, final Object o) {
		if (o == null) {
			return true;
		}
		final Class from = o.getClass();
		if (to.isAssignableFrom(from)) {
			return true;
		}
		return COERCABLE[classToType(from).ordinal()][classToType(to).ordinal()];
	}

	private final List<M>[] byArity = new List[11];;
	public String name;

	public InvokableHandle(final String name, final M[] invokables) {
		this.name = name;
		for (final M m : invokables) {
			final int arity = arity(getParameterTypes(m));
			if (byArity[arity] == null) {
				byArity[arity] = new ArrayList<M>(1);
			}
			byArity[arity].add(m);
		}
	}

	public M resolve(final Object[] arguments) throws MethodTypeMismatchException, InsufficientArgsException {
		final List<M> possible = byArity[arity(arguments)];
		if (possible == null) {
			throw new InsufficientArgsException("Wrong number of arguments.");
		}
		switch (possible.size()) {
		case 0:
			throw new MethodTypeMismatchException(name);
		case 1:
			final M out = possible.get(0);
			coerce(getParameterTypes(out), arguments);
			return out;
		default:
			for (final M m : possible) {
				final Class[] parameterTypes = getParameterTypes(m);
				// FIXME: should use a cache to speed up resolution
				// FIXME: should obey Java overloading rules
				if (resolve(parameterTypes, arguments)) {
					coerce(parameterTypes, arguments);
					return m;
				}
			}
		}
		throw new MethodTypeMismatchException(name);
	}

	protected abstract Class[] getParameterTypes(M m);

	protected abstract int getModifiers(M m);
}
