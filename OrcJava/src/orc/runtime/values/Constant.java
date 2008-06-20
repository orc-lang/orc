/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import orc.runtime.Token;
import orc.runtime.sites.java.ObjectProxy;

/**
 * A value container for a literal value
 * @author wcook, dkitchin
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
	public orc.orchard.oil.Value marshal() {
		return new orc.orchard.oil.Constant(value);
	}
}
