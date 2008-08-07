/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.ConsValue;
import orc.runtime.values.ListValue;

/**
 * Implements the "cons" constructor site.
 * 
 * @author dkitchin
 */
public class Cons extends EvalSite {

	@Override
	public Object evaluate(Args args) throws TokenException {
		Object t = args.getArg(1);
		if (!(t instanceof ListValue)) {
			//throw new TokenException("Cons expects second argument to be a list value; got a value of type " + t.getClass().toString());
			throw new ArgumentTypeMismatchException(1, "List", t.getClass().toString());
		}
		return new ConsValue(args.getArg(0), (ListValue)t);
	}

}
