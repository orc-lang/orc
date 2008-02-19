/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * A tuple value container
 * @author wcook
 */
public class Tuple extends EvalSite {
	private static final long serialVersionUID = 1L;

	List<Value> values;

	/* Constructor for the empty tuple */
	public Tuple() {
		this.values = new LinkedList<Value>();
	}
	
	public Tuple(List<Value> values) {
		this.values = values;
	}
	
	// A tuple may be used as a site for array lookup
	public Value evaluate(Args args)
	{
		int i = args.intArg(0);
		
		// Uses Perl's idiom for end-lookup using negative integers
		if (i < 0) { i = size() + i; }
		
		// Note that this will still be out of bounds if |i| >= size
		return at(i);
	}
	
	public String toString() {
		return format('(', values, ", ", ')');
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
	
}

