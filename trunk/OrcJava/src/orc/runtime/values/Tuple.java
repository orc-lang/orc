/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.List;

import orc.runtime.sites.EvalSite;

/**
 * A tuple value container
 * @author wcook
 */
public class Tuple extends EvalSite {
	private static final long serialVersionUID = 1L;
	Object[] values;

	public Tuple(Object[] values) {
		this.values = values;
	}

	public Object at(int i) {
		return values[i];
	}
	
	// A tuple may be used as a site for array lookup
	public Object evaluate(Object[] args)
	{
		int i = intArg(args,0);
		
		// Uses Perl's idiom for end-lookup using negative integers
		if (i < 0) { i = size() + i; }
		
		// Note that this will still be out of bounds if |i| >= size
		return at(i);
	}
	
	public String toString() {
		return format('[', Arrays.asList(values), ", ", ']');
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
		return values.length;
	}
}
