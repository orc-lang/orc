package orc.runtime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import orc.error.ArgumentTypeMismatchException;
import orc.error.ArityMismatchException;
import orc.error.InsufficientArgsException;
import orc.error.OrcError;
import orc.error.TokenException;
import orc.runtime.values.Constant;
import orc.runtime.values.Field;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

/**
 * 
 * Container for arguments to a site. 
 * 
 * @author dkitchin
 *
 */

public class Args implements Serializable {
	Value[] values;
	
	public Args(List<Value> values) {
		this.values = new Value[values.size()];
		this.values = values.toArray(this.values);
	}
	
	public int size() {
		return values.length;
	}
	
	/**
	 * Classic 'let' functionality. 
	 * Reduce a list of argument values into a single value as follows:
	 * 
	 * Zero arguments: return a signal
	 * One argument: return that value
	 * Two or more arguments: return a tuple of values
	 * 
	 */
	public Value condense() {
		if (values.length == 0) {
			return Value.signal();
		} else if (values.length == 1) {
			return values[0];
		} else {
			return new TupleValue(values);
		}
	}
	
	/**
	 * Helper function to retrieve the nth value (starting from 0), with error
	 * checking
	 */
	public Value valArg(int n) throws TokenException {
		try {
			return values[n];
		} catch (ArrayIndexOutOfBoundsException e) {
			//throw new TokenException("Arity mismatch calling site. Could not find argument #" + n);
			throw new InsufficientArgsException(n, values.length);
		}
	}
	
	public String fieldName() throws TokenException {
		if (values.length != 1) {
			//throw new TokenException("Arity mismatch resolving field reference.");
			throw new ArityMismatchException(1, values.length);
		}
		Value v = values[0];
		if (v instanceof Field) {
			return ((Field)v).getKey();
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
	public Object getArg(int n) throws TokenException
	{
		Value a;
		
		try {
			a = values[n];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			//throw new TokenException("Arity mismatch calling site. Could not find argument #" + n);
			throw new InsufficientArgsException(n, values.length);
		}
		
		try {
			return ((Constant)a).getValue();
		}
		catch (ClassCastException e) {
			//throw new TokenException("Argument " + n + " to site is not a native Java value");
			throw new ArgumentTypeMismatchException(n, "native Java value", a.getClass().toString());
		} 
	}
	
	/* Return the entire tuple as an object array */
	public Object[] asArray() throws TokenException {
		int n = values.length;
		Object[] a = new Object[n];
		for (int i=0; i<n; i++) { 
			a[i] = getArg(i);
		}
		return a;
	}
		
	/**
	 * Helper function for integers
	 * @throws TokenException 
	 */
	public int intArg(int n) throws TokenException {
		
		Object a = getArg(n);
		try
			{ return ((Number)a).intValue(); }
		catch (ClassCastException e) { 
			// throw new TokenException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); 
			throw new ArgumentTypeMismatchException(n, "int", a.getClass().toString());
		} 
	}

	/**
	 * Helper function for longs
	 * @throws TokenException 
	 */
	public long longArg(int n) throws TokenException {
		
		Object a = getArg(n);
		try
			{ return ((Number)a).longValue(); }
		catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "long", a.getClass().toString());
		}
			// { throw new TokenException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	public Number numberArg(int n) throws TokenException {
		Object a = getArg(n);
		try
			{ return (Number)a; }
		catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "Number", a.getClass().toString());
		}
	}
	
	/**
	 * Helper function for booleans
	 * @throws TokenException 
	 */
	public boolean boolArg(int n) throws TokenException {
		
		Object a = getArg(n);
		try
			{ return ((Boolean)a).booleanValue(); }
		catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "bool", a.getClass().toString());
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
	public String stringArg(int n) throws TokenException {
		Object a = getArg(n);
		try {
			return (String)a;
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "String", a.getClass().toString());
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
	public static <T> T applyNumericOperator(Number a, Number b, NumericBinaryOperator<T> op) {
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
			throw new OrcError("Unexpected Number type in ("
					+ a.getClass().toString()
					+ ", " + b.getClass().toString() + ")");
		}
	}

	/**
	 * Dispatch a unary operator based on the type of a number.
	 */
	public static <T> T applyNumericOperator(Number a, NumericUnaryOperator<T> op) {
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
			throw new OrcError("Unexpected Number type in ("
					+ a.getClass().toString() + ")");
		}
	}
}
