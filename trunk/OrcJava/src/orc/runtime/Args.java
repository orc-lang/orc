package orc.runtime;

import java.io.Serializable;
import java.util.List;

import orc.error.ArgumentTypeMismatchException;
import orc.error.ArityMismatchException;
import orc.error.InsufficientArgsException;
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
			{ return ((Integer)a).intValue(); }
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
			{ return ((Integer)a).longValue(); }
		catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "long", a.getClass().toString());
		}
			// { throw new TokenException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
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
	 * Helper function for strings
	 * @throws TokenException 
	 */
	public String stringArg(int n) throws TokenException {

		Object a = getArg(n);
		return a.toString();
	}
	
}
