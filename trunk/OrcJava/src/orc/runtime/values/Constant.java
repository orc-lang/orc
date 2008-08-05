/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import kilim.Pausable;
import kilim.Task;

import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.Kilim.PausableCallable;
import orc.runtime.Kilim.Promise;
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
	
	/**
	 * ListValue view for iterators. Because iterators
	 * are not cloneable and are mutable, we have to cache
	 * the head and tail.
	 * @author quark
	 */
	private static class IteratorListValue extends LazyListValue {
		private Iterator iterator;
		private TupleValue cons = null;
		private boolean forced = false;
		public IteratorListValue(Iterator iterator) {
			this.iterator = iterator;
		}
		private void force() {
			if (forced) return;
			forced = true;
			if (iterator.hasNext()) {
				cons = new TupleValue(
					new Constant(iterator.next()),
					new IteratorListValue(iterator));
			}
		}
		@Override
		public void uncons(Token caller) {
			force();
			if (cons == null) caller.die();
			else caller.resume(cons);
		}
		@Override
		public void unnil(Token caller) {
			force();
			if (cons == null) caller.resume(new NilValue());
			else caller.die();
		}
	}

	@Override
	public void uncons(Token caller) {
		try {
			new IteratorListValue(asIterator()).uncons(caller);
		} catch (ClassCastException e) {
			caller.die();
		}
	}
	
	@Override
	public void unnil(Token caller) {
		try {
			new IteratorListValue(asIterator()).unnil(caller);
		} catch (ClassCastException e) {
			caller.die();
		}
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
