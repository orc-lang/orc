package orc.runtime;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.values.Constant;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

/**
 * 
 * Container for arguments to a site. 
 * 
 * @author dkitchin
 *
 */

public class Args {

	List<Value> values;
	
	public Args(List<Value> values)
	{
		this.values = values;
	}
	
	public Args()
	{
		this.values = new LinkedList<Value>();
	}
	
	public List<Value> getValues()
	{
		return values;
	}
	
	public int size()
	{
		return values.size();
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
		
		if (values.size() == 0) {
			return Value.signal();
		}
		else if (values.size() == 1) {
			return values.get(0);
		}
		else {
			return new TupleValue(values);
		}
		
	}
	
	/**
	 * Helper function to retrieve the nth value, with error checking
	 */
	public Value valArg(int n) throws OrcRuntimeTypeError
	{
		try {
			return values.get(n);
		}
		catch (IndexOutOfBoundsException e)
			{ throw new OrcRuntimeTypeError("Arity mismatch calling site. Could not find argument #" + n); }
	}
	
	
	/**
	 * Helper function to retrieve the nth element as an object, with error checking
	 * @throws OrcRuntimeTypeError 
	 */
	public Object getArg(int n) throws OrcRuntimeTypeError
	{
		try {
			Value a = values.get(n);
			return ((Constant)a).getValue(); 
		}
		catch (IndexOutOfBoundsException e)
			{ throw new OrcRuntimeTypeError("Arity mismatch calling site. Could not find argument #" + n); }
		catch (ClassCastException e) 
			{ throw new OrcRuntimeTypeError("Argument " + n + " to site is not a native Java value"); } 
	}
	
	/* Return the entire tuple as an object array */
	public Object[] asArray() throws OrcRuntimeTypeError
	{
		int n = values.size();
		Object[] a = new Object[n];
		for (int i=0; i<n; i++)
		{ 
			a[i] = getArg(i); 
		}
		
		return a;
	}
		
	/**
	 * Helper function for integers
	 * @throws OrcRuntimeTypeError 
	 */
	public int intArg(int n) throws OrcRuntimeTypeError {
		
		Object a = getArg(n);
		try
			{ return ((Integer)a).intValue(); }
		catch (ClassCastException e) 
			{ throw new OrcRuntimeTypeError("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	/**
	 * Helper function for longs
	 * @throws OrcRuntimeTypeError 
	 */
	public long longArg(int n) throws OrcRuntimeTypeError {
		
		Object a = getArg(n);
		try
			{ return ((Integer)a).longValue(); }
		catch (ClassCastException e) 
			{ throw new OrcRuntimeTypeError("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	
	/**
	 * Helper function for booleans
	 * @throws OrcRuntimeTypeError 
	 */
	public boolean boolArg(int n) throws OrcRuntimeTypeError {
		
		Object a = getArg(n);
		try
			{ return ((Boolean)a).booleanValue(); }
		catch (ClassCastException e) 
			{ throw new OrcRuntimeTypeError("Argument " + n + " to site '" + this.toString() + "' should be a boolean, got " + a.getClass().toString() + " instead."); } 
	
	}

	/**
	 * Helper function for strings
	 * @throws OrcRuntimeTypeError 
	 */
	public String stringArg(int n) throws OrcRuntimeTypeError {

		Object a = getArg(n);
		return a.toString();
	}
	
}
