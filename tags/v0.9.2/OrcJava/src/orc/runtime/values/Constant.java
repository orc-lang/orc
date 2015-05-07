/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import orc.runtime.Token;
import orc.runtime.sites.java.ObjectProxy;

/**
 * A value container for an arbitrary Java object, which includes Orc literal
 * atomic values.
 * 
 * <p>
 * This has overrides to support treating collections and arrays as lists (i.e.
 * list pattern matches will work on them).
 * 
 * <p>FIXME: how well does this cope with Java nulls?
 * 
 * @author wcook, dkitchin, quark
 */
public class Constant extends Value {
	private static final long serialVersionUID = 1L;
	Object value;

	public Constant(Object value) {
		this.value = value;
	}

	public Object getValue()
	{
		return value;
	}
	
	public String toString() {
		return value.toString();
	}
	
	/**
	 * A Java value used in call position becomes a proxied object.
	 */
	public Callable forceCall(Token t)
	{
		return new ObjectProxy(value);
	}
	
	/**
	 * Produce a new iterator for this value.
	 * Throws an exception if the value is not iterable.
	 */
	private Iterator asIterator() throws ClassCastException {
		if (value == null) {
			throw new ClassCastException("Null value cannot be deconstructed.");
		}
		try {
			return ((Iterable)value).iterator();
		} catch (ClassCastException _) {
			return Arrays.asList((Object[])value).iterator();
		}
	}

	@Override
	public Value head() {
		return new Constant(asIterator().next());
	}

	@Override
	public Value tail() {
		// Copy the remainder of the constant to an Orc list.
		// This seems awfully inefficient, but I can't think
		// of a better way to do it without being able to clone
		// iterators.
		ListValue l = new NilValue();
		Iterator i = asIterator();
		LinkedList<Value> tmpList = new LinkedList<Value>();
		i.next();
		while (i.hasNext()) tmpList.add(new Constant(i.next()));
		return ListValue.make(tmpList);
	}

	@Override
	public boolean isCons() {
		try {
			Iterator i = asIterator();
			return i.hasNext();
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public boolean isNil() {
		try {
			Iterator i = asIterator();
			return !i.hasNext();
		} catch (ClassCastException e) {
			return false;
		}
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
