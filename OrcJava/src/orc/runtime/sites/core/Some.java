/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.runtime.RuntimeTypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.Constructor;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.OptionValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.TupleValue;

/**
 * Implements the "some" option constructor site.
 * 
 * @author dkitchin
 */
public class Some extends Constructor {
	@Override
	public Object construct(Args args) throws TokenException {
		return new SomeValue(args.getArg(0));
	}

	@Override
	public TupleValue deconstruct(Object arg) throws TokenException {
		if (!(arg instanceof OptionValue))
			throw new RuntimeTypeException(arg.getClass() + " is not an instanceof OptionValue");
		if (!(arg instanceof SomeValue)) return null;
		return new TupleValue(((SomeValue)arg).content);
	}
}
