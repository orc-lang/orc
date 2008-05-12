package orc.runtime;

import java.io.Serializable;
import java.util.List;

import orc.error.OrcRuntimeTypeException;
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
		}
		else if (values.length == 1) {
			return values[0];
		}
		else {
			return new TupleValue(values);
		}
	}
	
	/**
	 * Helper function to retrieve the nth value (starting from 0), with error
	 * checking
	 */
	public Value valArg(int n) throws OrcRuntimeTypeException {
		try {
			return values[n];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new OrcRuntimeTypeException("Arity mismatch calling site. Could not find argument #" + n);
		}
	}
	
	public String fieldName() throws OrcRuntimeTypeException {
		if (values.length != 1) {
			throw new OrcRuntimeTypeException("Arity mismatch resolving field reference.");
		}
		Value v = values[0];
		if (v instanceof Field) {
			return ((Field)v).getKey();
		} else {
			throw new OrcRuntimeTypeException("Bad type for field reference.");
		}
	}
	
	/**
	 * Helper function to retrieve the nth element as an object (starting from
	 * 0), with error checking
	 * 
	 * @throws OrcRuntimeTypeException
	 */
	public Object getArg(int n) throws OrcRuntimeTypeException
	{
		try {
			Value a = values[n];
			return ((Constant)a).getValue(); 
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new OrcRuntimeTypeException("Arity mismatch calling site. Could not find argument #" + n);
		}
		catch (ClassCastException e) {
			throw new OrcRuntimeTypeException("Argument " + n + " to site is not a native Java value");
		} 
	}
	
	/* Return the entire tuple as an object array */
	public Object[] asArray() throws OrcRuntimeTypeException {
		int n = values.length;
		Object[] a = new Object[n];
		for (int i=0; i<n; i++) { 
			a[i] = getArg(i);
		}
		return a;
	}
		
	/**
	 * Helper function for integers
	 * @throws OrcRuntimeTypeException 
	 */
	public int intArg(int n) throws OrcRuntimeTypeException {
		Object a = getArg(n);
		try
			{ return ((Integer)a).intValue(); }
		catch (ClassCastException e) 
			{ throw new OrcRuntimeTypeException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	/**
	 * Helper function for longs
	 * @throws OrcRuntimeTypeException 
	 */
	public long longArg(int n) throws OrcRuntimeTypeException {
		Object a = getArg(n);
		try
			{ return ((Integer)a).longValue(); }
		catch (ClassCastException e) 
			{ throw new OrcRuntimeTypeException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	
	/**
	 * Helper function for booleans
	 * @throws OrcRuntimeTypeException 
	 */
	public boolean boolArg(int n) throws OrcRuntimeTypeException {
		Object a = getArg(n);
		try
			{ return ((Boolean)a).booleanValue(); }
		catch (ClassCastException e) 
			{ throw new OrcRuntimeTypeException("Argument " + n + " to site '" + this.toString() + "' should be a boolean, got " + a.getClass().toString() + " instead."); } 
	
	}

	/**
	 * Helper function for strings
	 * @throws OrcRuntimeTypeException 
	 */
	public String stringArg(int n) throws OrcRuntimeTypeException {
		Object a = getArg(n);
		return a.toString();
	}
}