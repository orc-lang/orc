/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.core.Equal;

/**
 * A tuple value container
 * @author wcook, quark
 */
public class TupleValue extends DotSite implements Iterable<Object>, Eq {
	public Object[] values;
	public TupleValue(List<Object> values) {
		this.values = new Object[values.size()];
		this.values = values.toArray(this.values);
	}
	public TupleValue(Object ... values) {
		this.values = values;
	}
	
	@Override
	protected void addMembers() {
		addMember("fits", new PartialSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				return (args.intArg(0) == values.length ? Value.signal() : null);
			}
		});
	}
	
	@Override
	protected void defaultTo(Args args, Token token) throws TokenException {
		try {
			token.resume(values[args.intArg(0)]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new JavaException(e);
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
		return eqTo(that_);
	}
	public boolean eqTo(Object that_) {
		if (!(that_ instanceof TupleValue)) return false;
		TupleValue that = (TupleValue)that_;
		if (that.values.length != this.values.length) return false;
		for (int i = 0; i < this.values.length; ++i) {
			if (!Equal.eq(this.values[i], that.values[i])) return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}
}