/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.runtime.sites.EvalSite;

/**
 * A tuple value container
 * @author wcook
 */
public class Tuple extends EvalSite {
	private static final long serialVersionUID = 1L;
	List<Value> values;

	public Tuple(List<Value> values) {
		this.values = values;
	}
	
	// A tuple may be used as a site for array lookup
	public Value evaluate(Tuple args)
	{
		int i = args.intArg(0);
		
		// Uses Perl's idiom for end-lookup using negative integers
		if (i < 0) { i = size() + i; }
		
		// Note that this will still be out of bounds if |i| >= size
		return at(i);
	}
	
	public String toString() {
		return format('[', values, ", ", ']');
	}

	public static String format(char left, List items, String sep, char right) {
		StringBuffer buf = new StringBuffer();
		buf.append(left);
		int i = 0;
		for (Object x : items) {
			if (i > 0)
				buf.append(sep);
			buf.append(x);
			i++;
		}
		buf.append(right);
		return buf.toString();
	}

	public int size() {
		return values.size();
	}
	
	
	public Value at(int i) {
		return values.get(i);
	}
	
	/**
	 * Helper function to retrieve the nth element, with error checking
	 */
	public Object getArg(int n)
	{
		Value a = at(n);
		try
		 	{ return ((Constant)a).getValue(); }
		catch (ArrayIndexOutOfBoundsException e)
			{ throw new Error("Arity mismatch calling site '" + this.toString() + "'. Could not find argument #" + n); }
		catch (ClassCastException e) 
			{ throw new Error("Argument " + n + " to site '" + this.toString() + "' is not a native Java value"); } 
	}
	
	/* Return the entire tuple as an object array */
	public Object[] asArray()
	{
		int n = size();
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

