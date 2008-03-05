/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.Args;
import orc.runtime.OrcRuntimeTypeError;
import orc.runtime.sites.EvalSite;

/**
 * A tuple value container
 * @author wcook
 */
public class TupleValue extends EvalSite {
	private static final long serialVersionUID = 1L;

	List<Value> values;

	/* Constructor for the empty tuple */
	public TupleValue() {
		this.values = new LinkedList<Value>();
	}
	
	/* Constructor for binary tuples */
	public TupleValue(Value v, Value w) {
		this.values = new LinkedList<Value>();
		this.values.add(v);
		this.values.add(w);
	}
	
	public TupleValue(List<Value> values) {
		this.values = values;
	}
	
	class FitSite extends EvalSite {
		
		int size;
		public FitSite(int size) {
			this.size = size;
		}
		
		public Value evaluate(Args args) throws OrcRuntimeTypeError {
			return new Constant(args.intArg(0) == this.size);
		}
	}
	
	// A tuple may be used as a site for array lookup
	public Value evaluate(Args args) throws OrcRuntimeTypeError
	{
		// TODO: Generalize this treatment of dot sites.
		try {			
			String s = args.stringArg(0);
			
			if (s.equals("fits")) {
				return new FitSite(values.size());
			}
			else { throw new Exception(); }
		}
		catch (Exception e) {
		int i = args.intArg(0);
		
		return at(i);
		}
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
			buf.append(x.toString());
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

