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
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
