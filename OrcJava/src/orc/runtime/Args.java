package orc.runtime;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.values.Constant;
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
	 * Helper function to retrieve the nth value, with error checking
	 */
	public Value valArg(int n)
	{
		try {
			return values.get(n);
		}
		catch (IndexOutOfBoundsException e)
			{ throw new Error("Arity mismatch calling site. Could not find argument #" + n); }
	}
	
	
	/**
	 * Helper function to retrieve the nth element as an object, with error checking
	 */
	public Object getArg(int n)
	{
		try {
			Value a = values.get(n);
			return ((Constant)a).getValue(); 
		}
		catch (IndexOutOfBoundsException e)
			{ throw new Error("Arity mismatch calling site. Could not find argument #" + n); }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " to site is not a native Java value"); } 
	}
	
	/* Return the entire tuple as an object array */
	public Object[] asArray()
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
	 */
	public int intArg(int n) {
		
		Object a = getArg(n);
		try
			{ return ((Integer)a).intValue(); }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	/**
	 * Helper function for longs
	 */
	public long longArg(int n) {
		
		Object a = getArg(n);
		try
			{ return ((Integer)a).longValue(); }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	
	/**
	 * Helper function for booleans
	 */
	public boolean boolArg(int n) {
		
		Object a = getArg(n);
		try
			{ return ((Boolean)a).booleanValue(); }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " to site '" + this.toString() + "' should be a boolean, got " + a.getClass().toString() + " instead."); } 
	
	}

	/**
	 * Helper function for strings
	 */
	public String stringArg(int n) {

		Object a = getArg(n);
		return a.toString();
	}
	
}
