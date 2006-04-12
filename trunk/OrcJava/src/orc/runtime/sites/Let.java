/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Tuple;

/**
 * Implements the built-in "let" site
 * @author wcook
 */
public class Let extends Site {

	/**
	 *  Outputs a single value or creates a tuple.
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	void callSite(Object[] args, Token returnToken, OrcEngine engine) {

		Object value = (args.length == 1) ? args[0] : new Tuple(args);
		returnToken.setResult(value);
		engine.activate(returnToken);
	}
}
