//
// Args.java -- Java class Args
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.InsufficientArgsException;
import orc.error.runtime.JavaException;
import orc.values.Field;

/**
 * Container for arguments to a site.
 * 
 * @author dkitchin
 */
@SuppressWarnings("serial")
public class Args implements Serializable {
	Object[] values;

	public Args(@SuppressWarnings("hiding") final List<Object> values) {
		this.values = new Object[values.size()];
		this.values = values.toArray(this.values);
	}

	/**
	 * @return number of arguments
	 */
	public int size() {
		return values.length;
	}

	/**
	 * Helper function to, assuming the argument is an Orc field, retrieve the
	 * field's name
	 * 
	 * @return
	 * @throws ArityMismatchException
	 * @throws ArgumentTypeMismatchException
	 */
	public String fieldName() throws ArityMismatchException, ArgumentTypeMismatchException {
		if (values.length != 1) {
			throw new ArityMismatchException(1, values.length);
		}
		final Object v = values[0];
		if (v == null) {
			throw new ArgumentTypeMismatchException(0, "message", "null");
		}
		if (v instanceof Field) {
			return ((Field) v).field();
		} else {
			throw new ArgumentTypeMismatchException(0, "message", v != null ? v.getClass().toString() : "null");
		}
	}

	/**
	 * Helper function to retrieve the nth element as an object (starting from
	 * 0), with error checking
	 * 
	 * @param n
	 * @return
	 * @throws InsufficientArgsException
	 */
	public Object getArg(final int n) throws InsufficientArgsException {
		try {
			return values[n];
		} catch (final ArrayIndexOutOfBoundsException e) {
			throw new InsufficientArgsException(n, values.length);
		}
	}

	/**
	 * Return the entire tuple as an object array. Please don't mutate the
	 * array.
	 * 
	 * @return
	 */
	public Object[] asArray() {
		return values;
	}

	/**
	 * Helper function for integers
	 * 
	 * @param n
	 * @return
	 * @throws ArgumentTypeMismatchException
	 * @throws InsufficientArgsException
	 */
	public int intArg(final int n) throws ArgumentTypeMismatchException, InsufficientArgsException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "32-bit signed integer", "null");
		}
		try {

			return ((Number) a).intValue();
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "32-bit signed integer", a != null ? a.getClass().toString() : "null");
		}
	}

	/**
	 * Helper function for longs
	 * 
	 * @param n
	 * @return
	 * @throws ArgumentTypeMismatchException
	 * @throws InsufficientArgsException
	 */
	public long longArg(final int n) throws ArgumentTypeMismatchException, InsufficientArgsException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "64-bit signed integer", "null");
		}
		try {
			return ((Number) a).longValue();
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "64-bit signed integer", a != null ? a.getClass().toString() : "null");
		}
	}

	/**
	 * @param n
	 * @return
	 * @throws ArgumentTypeMismatchException
	 * @throws InsufficientArgsException
	 */
	public Number numberArg(final int n) throws ArgumentTypeMismatchException, InsufficientArgsException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "Number", "null");
		}
		try {
			return (Number) a;
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "Number", a != null ? a.getClass().toString() : "null");
		}
	}

	/**
	 * Helper function for booleans
	 * 
	 * @param n
	 * @return
	 * @throws ArgumentTypeMismatchException
	 * @throws InsufficientArgsException
	 */
	public boolean boolArg(final int n) throws ArgumentTypeMismatchException, InsufficientArgsException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "boolean", "null");
		}
		try {
			return ((Boolean) a).booleanValue();
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "boolean", a != null ? a.getClass().toString() : "null");
		}
	}

	/**
	 * Helper function for strings. Note that this requires a strict String
	 * type. If you don't care whether the argument is really a string, use
	 * valArg(n).toString().
	 * 
	 * @param n
	 * @return
	 * @throws ArgumentTypeMismatchException
	 * @throws InsufficientArgsException
	 */
	public String stringArg(final int n) throws ArgumentTypeMismatchException, InsufficientArgsException {
		final Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "String", "null");
		}
		try {
			return (String) a;
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "String", a != null ? a.getClass().toString() : "null");
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
	 * Dispatch a binary operator based on the widest type of two numbers.
	 * 
	 * @param <T>
	 * @param a
	 * @param b
	 * @param op
	 * @return
	 * @throws JavaException
	 */
	public static <T> T applyNumericOperator(final Number a, final Number b, final NumericBinaryOperator<T> op) throws JavaException {
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
				throw new JavaException(new IllegalArgumentException("Unexpected Number type in (" + (a != null ? a.getClass().toString() : "null") + ", " + (b != null ? b.getClass().toString() : "null") + ")"));
			}
		} catch (final ArithmeticException e) {
			throw new JavaException(e);
		}
	}

	/**
	 * Dispatch a unary operator based on the type of a number.
	 * 
	 * @param <T>
	 * @param a
	 * @param op
	 * @return
	 * @throws JavaException
	 */
	public static <T> T applyNumericOperator(final Number a, final NumericUnaryOperator<T> op) throws JavaException {
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
			throw new JavaException(new IllegalArgumentException("Unexpected Number type in (" + (a != null ? a.getClass().toString() : "null") + ")"));
		}
	}
}
