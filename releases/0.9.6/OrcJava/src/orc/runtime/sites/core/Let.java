/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;
import orc.type.Type;

/**
 * Implements the built-in "let" site
 * @author wcook
 */
public class Let extends Site {
  private static final long serialVersionUID = 1L;

	/**
	 * Outputs a single value or creates a tuple.
	 */
	public void callSite(Args args, Token caller) {
		// Note that a let does not resume like a normal site call; it sets the result and activates directly;
		// This is necessary to preserve the "immediate" semantics of Let.
		caller.setResult(condense(args.asArray()));
		// Need to trace the return even though it's weird
		caller.getTracer().receive(caller.getResult());
		// Activate the token
		caller.activate();
	}
	
	public Type type() throws TypeException {
		return Type.LET;
	}
	
	/**
	 * Classic 'let' functionality. 
	 * Reduce a list of argument values into a single value as follows:
	 * 
	 * Zero arguments: return a signal
	 * One argument: return that value
	 * Two or more arguments: return a tuple of values
	 * 
	 */
	public static Object condense(Object[] values) {
		if (values.length == 0) {
			return Value.signal();
		} else if (values.length == 1) {
			return values[0];
		} else {
			return new TupleValue(values);
		}
	}
	
}
