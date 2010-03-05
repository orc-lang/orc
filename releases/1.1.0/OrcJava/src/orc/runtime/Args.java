//
// Args.java -- Java class Args
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

package orc.runtime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import orc.error.OrcError;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.InsufficientArgsException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.sites.core.Let;
import orc.runtime.values.Field;
import orc.runtime.values.ListLike;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

/**
 * Container for arguments to a site. 
 * 
 * @author dkitchin
 */
public class Args implements Serializable, Iterable<Object> {
	Object[] values;

	public Args(final List<Object> values) {
		this.values = new Object[values.size()];
		this.values = values.toArray(this.values);
	}

	public Args(final Object[] values) {
		this.values = values;
	}

	public int size() {
		return values.length;
	}

	public Object condense() {
		return Let.condense(values);
	}

	/**
	 * Helper function to retrieve the nth value (starting from 0), with error
	 * checking.
	 * @deprecated
	 */
	@Deprecated
	public Value valArg(final int n) throws TokenException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "Value", "null");
		}
		try {
			return (Value) a;
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "Value", a.getClass().toString());
		}
	}

	public String fieldName() throws TokenException {
		if (values.length != 1) {
			//throw new TokenException("Arity mismatch resolving field reference.");
			throw new ArityMismatchException(1, values.length);
		}
		final Object v = values[0];
		if (v == null) {
			throw new ArgumentTypeMismatchException(0, "message", "null");
		}
		if (v instanceof Field) {
			return ((Field) v).getKey();
		} else {
			//throw new TokenException("Bad type for field reference.");
			throw new ArgumentTypeMismatchException(0, "message", v.getClass().toString());
		}
	}

	/**
	 * Helper function to retrieve the nth element as an object (starting from
	 * 0), with error checking
	 * 
	 * @throws TokenException
	 */
	public Object getArg(final int n) throws TokenException {
		try {
			return values[n];
		} catch (final ArrayIndexOutOfBoundsException e) {
			throw new InsufficientArgsException(n, values.length);
		}
	}

	/**
	 * Return the entire tuple as an object array.
	 * Please don't mutate the array.
	 */
	public Object[] asArray() {
		return values;
	}

	/**
	 * Helper function for integers
	 * @throws TokenException 
	 */
	public int intArg(final int n) throws TokenException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "int", "null");
		}
		try {
			return ((Number) a).intValue();
		} catch (final ClassCastException e) {
			// throw new TokenException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); 
			throw new ArgumentTypeMismatchException(n, "int", a.getClass().toString());
		}
	}

	/**
	 * Helper function for longs
	 * @throws TokenException 
	 */
	public long longArg(final int n) throws TokenException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "long", "null");
		}
		try {
			return ((Number) a).longValue();
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "long", a.getClass().toString());
		}
		// { throw new TokenException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	public Number numberArg(final int n) throws TokenException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "Number", "null");
		}
		try {
			return (Number) a;
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "Number", a.getClass().toString());
		}
	}

	/**
	 * Helper function for booleans
	 * @throws TokenException 
	 */
	public boolean boolArg(final int n) throws TokenException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "boolean", "null");
		}
		try {
			return ((Boolean) a).booleanValue();
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "boolean", a.getClass().toString());
		}
		//{ throw new TokenException("Argument " + n + " to site '" + this.toString() + "' should be a boolean, got " + a.getClass().toString() + " instead."); } 

	}

	/**
	 * Helper function for strings.
	 * Note that this requires a strict String type.
	 * If you don't care whether the argument is really a string,
	 * use valArg(n).toString().
	 * @throws TokenException 
	 */
	public String stringArg(final int n) throws TokenException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "String", "null");
		}
		try {
			return (String) a;
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "String", a.getClass().toString());
		}
	}

	private static class StringListValue implements ListLike {
		private final String value;

		public StringListValue(final String value) {
			this.value = value;
		}

		public void uncons(final Token caller) {
			if (value.equals("")) {
				caller.die();
			} else {
				caller.resume(new TupleValue(value.substring(0, 1), value.substring(1)));
			}
		}

		public void unnil(final Token caller) {
			if (value.equals("")) {
				caller.resume(Value.signal());
			} else {
				caller.die();
			}
		}
	}

	/**
	 * ListValue view for iterators. Because iterators are not cloneable and are
	 * mutable, we have to cache the head and tail.
	 * 
	 * @author quark
	 */
	private static class IteratorListValue implements ListLike {
		private final Iterator iterator;

		private TupleValue cons = null;

		private boolean forced = false;

		public IteratorListValue(final Iterator iterator) {
			this.iterator = iterator;
		}

		private void force() {
			if (forced) {
				return;
			}
			forced = true;
			if (iterator.hasNext()) {
				cons = new TupleValue(iterator.next(), new IteratorListValue(iterator));
			}
		}

		public void uncons(final Token caller) {
			force();
			if (cons == null) {
				caller.die();
			} else {
				caller.resume(cons);
			}
		}

		public void unnil(final Token caller) {
			force();
			if (cons == null) {
				caller.resume(Value.signal());
			} else {
				caller.die();
			}
		}
	}

	public ListLike listLikeArg(final int n) throws TokenException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "ListLike", "null");
		} else if (a instanceof ListLike) {
			return (ListLike) a;
		} else if (a instanceof String) {
			return new StringListValue((String) a);
		} else if (a instanceof Iterable) {
			final Iterator it = ((Iterable) a).iterator();
			return new IteratorListValue(it);
		} else if (a instanceof Object[]) {
			// FIXME: we should be able to iterate
			// over primitive arrays, but that requires
			// a different approach (asList won't work
			// with primitive arrays).
			return new IteratorListValue(Arrays.asList((Object[]) a).iterator());
		} else {
			throw new ArgumentTypeMismatchException(n, "ListLike", a.getClass().toString());
		}
	}

	/** A unary operator on numbers */
	public interface NumericUnaryOperator<T> {
		public T apply(BigInteger a);

		public T apply(BigDecimal a);

		public T apply(int a);

		public T apply(long a);

		public T apply(byte a);

		public T apply(short a);

		public T apply(double a);

		public T apply(float a);
	}

	/** A binary operator on numbers */
	public interface NumericBinaryOperator<T> {
		public T apply(BigInteger a, BigInteger b);

		public T apply(BigDecimal a, BigDecimal b);

		public T apply(int a, int b);

		public T apply(long a, long b);

		public T apply(byte a, byte b);

		public T apply(short a, short b);

		public T apply(double a, double b);

		public T apply(float a, float b);
	}

	/**
	 * Dispatch a binary operator based on the widest
	 * type of two numbers.
	 */
	public static <T> T applyNumericOperator(final Number a, final Number b, final NumericBinaryOperator<T> op) throws TokenException {
		try {
			if (a instanceof BigDecimal) {
				if (b instanceof BigDecimal) {
					return op.apply((BigDecimal) a, (BigDecimal) b);
				} else {
					return op.apply((BigDecimal) a, BigDecimal.valueOf(b.doubleValue()));
				}
			} else if (b instanceof BigDecimal) {
				if (a instanceof BigDecimal) {
					return op.apply((BigDecimal) a, (BigDecimal) b);
				} else {
					return op.apply(BigDecimal.valueOf(a.doubleValue()), (BigDecimal) b);
				}
			} else if (a instanceof Double || b instanceof Double) {
				return op.apply(a.doubleValue(), b.doubleValue());
			} else if (a instanceof Float || b instanceof Float) {
				return op.apply(a.floatValue(), b.floatValue());
			} else if (a instanceof BigInteger) {
				if (b instanceof BigInteger) {
					return op.apply((BigInteger) a, (BigInteger) b);
				} else {
					return op.apply((BigInteger) a, BigInteger.valueOf(b.longValue()));
				}
			} else if (b instanceof BigInteger) {
				if (a instanceof BigInteger) {
					return op.apply((BigInteger) a, (BigInteger) b);
				} else {
					return op.apply(BigInteger.valueOf(a.longValue()), (BigInteger) b);
				}
			} else if (a instanceof Long || b instanceof Long) {
				return op.apply(a.longValue(), b.longValue());
			} else if (a instanceof Integer || b instanceof Integer) {
				return op.apply(a.intValue(), b.intValue());
			} else if (a instanceof Short || b instanceof Short) {
				return op.apply(a.shortValue(), b.shortValue());
			} else if (a instanceof Byte || b instanceof Byte) {
				return op.apply(a.byteValue(), b.byteValue());
			} else {
				throw new OrcError("Unexpected Number type in (" + a.getClass().toString() + ", " + b.getClass().toString() + ")");
			}
		} catch (final ArithmeticException e) {
			throw new JavaException(e);
		}
	}

	/**
	 * Dispatch a unary operator based on the type of a number.
	 */
	public static <T> T applyNumericOperator(final Number a, final NumericUnaryOperator<T> op) {
		if (a instanceof BigDecimal) {
			return op.apply((BigDecimal) a);
		} else if (a instanceof Double) {
			return op.apply(a.doubleValue());
		} else if (a instanceof Float) {
			return op.apply(a.floatValue());
		} else if (a instanceof BigInteger) {
			return op.apply((BigInteger) a);
		} else if (a instanceof Long) {
			return op.apply(a.longValue());
		} else if (a instanceof Integer) {
			return op.apply(a.intValue());
		} else if (a instanceof Short) {
			return op.apply(a.shortValue());
		} else if (a instanceof Byte) {
			return op.apply(a.byteValue());
		} else {
			throw new OrcError("Unexpected Number type in (" + a.getClass().toString() + ")");
		}
	}

	public Iterator<Object> iterator() {
		return Arrays.asList(values).iterator();
	}
}
