/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;

/**
 * A tuple value container
 * @author wcook, quark
 */
public class TupleValue extends EvalSite implements Iterable<Object> {
	public Object[] values;
	public TupleValue() {
		this.values = new Object[0];
	}
	public TupleValue(Object v) {
		this.values = new Object[1];
		this.values[0] = v;
	}
	public TupleValue(Object v, Object w) {
		this.values = new Object[2];
		this.values[0] = v;
		this.values[1] = w;
	}
	public TupleValue(List<Object> values) {
		this.values = new Object[values.size()];
		this.values = values.toArray(this.values);
	}
	public TupleValue(Object[] values) {
		this.values = values;
	}
	public Object evaluate(Args args) throws TokenException	{
		// TODO: Generalize this treatment of dot sites.
		try { 
			String s = args.fieldName();
			if (s.equals("fits")) {
				return new FitSite(values.length);
			}
		} catch (TokenException e) {
			// do nothing
		}
		return values[args.intArg(0)];
	}

	static class FitSite extends PartialSite {
		int size;
		public FitSite(int size) {
			this.size = size;
		}
		public Object evaluate(Args args) throws TokenException {
			return (args.intArg(0) == this.size ? Value.signal() : null);
		}
	}
	
	public Object at(int i) {
		return values[i];
	}
	
	public int size() {
		return values.length;
	}

	public String toString() {
		if (values.length == 0) return "signal";
		return format('(', values, ", ", ')');
	}
	
	public static String format(char left, Object[] items, String sep, char right) {
		StringBuffer buf = new StringBuffer();
		buf.append(left);
		for (int i = 0; i < items.length; ++i) {
			if (i > 0) buf.append(sep);
			buf.append(String.valueOf(items[i]));
		}
		buf.append(right);
		return buf.toString();
	}
	public List<Object> asList() {
		return Arrays.asList(values);
	}
	public Iterator<Object> iterator() {
		return asList().iterator();
	}
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public boolean equals(Object that_) {
		if (that_ == null) return false;
		if (!(that_ instanceof TupleValue)) return false;
		TupleValue that = (TupleValue)that_;
		if (that.values.length != this.values.length) return false;
		for (int i = 0; i < this.values.length; ++i) {
			if (!this.values[i].equals(that.values[i])) return false;
		}
		return true;
	}
}