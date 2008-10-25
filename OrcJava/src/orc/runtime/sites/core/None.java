/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.runtime.RuntimeTypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.Constructor;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.NoneValue;
import orc.runtime.values.OptionValue;
import orc.runtime.values.TupleValue;

/**
 * Implements the "none" option constructor site.
 * 
 * @author dkitchin
 */
public class None extends Constructor {
	@Override
	public Object construct(Args args) throws TokenException {
		return new NoneValue();
	}

	@Override
	public Object deconstruct(Object arg) throws TokenException {
		if (!(arg instanceof OptionValue))
			throw new RuntimeTypeException(arg.getClass() + " is not an instanceof OptionValue");
		if (!(arg instanceof NoneValue)) return null;
		return signal();
	}
}
