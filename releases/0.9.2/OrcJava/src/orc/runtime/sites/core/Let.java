/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

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
		caller.setResult(args.condense()).activate();
	}

}
