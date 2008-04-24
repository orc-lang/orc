/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.ConsValue;
import orc.runtime.values.SomeValue;
import orc.runtime.values.Value;

/**
 * Implements the "cons" constructor site.
 * 
 * @author dkitchin
 */
public class Cons extends EvalSite {

	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		
		return new ConsValue(args.valArg(0), args.valArg(1));
	}

}
