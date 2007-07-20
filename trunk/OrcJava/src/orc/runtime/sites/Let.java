/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Tuple;
import orc.runtime.values.Value;

/**
 * Implements the built-in "let" site
 * @author wcook
 */
public class Let extends Site {
  private static final long serialVersionUID = 1L;

	/**
	 *  Outputs a single value or creates a tuple.
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void callSite(Tuple args, Token returnToken, GroupCell caller, OrcEngine engine) {

		Value value = (args.size() == 1) ? args.at(0) : args;
		returnToken.setResult(value);
		engine.activate(returnToken);
	}

}
