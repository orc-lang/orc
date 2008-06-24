/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.ArgumentTypeMismatchException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.ConsValue;
import orc.runtime.values.ListValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.Value;

/**
 * Implements the "cons" constructor site.
 * 
 * @author dkitchin
 */
public class Cons extends EvalSite {

	@Override
	public Value evaluate(Args args) throws TokenException {
		Value t = args.valArg(1);
		if (!(t instanceof ListValue)) {
			//throw new TokenException("Cons expects second argument to be a list value; got a value of type " + t.getClass().toString());
			throw new ArgumentTypeMismatchException(1, "List", t.getClass().toString());
		}
		return new ConsValue(args.valArg(0), (ListValue)t);
	}

}
