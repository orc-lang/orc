//
// PartialSite.java -- Java class PartialSite
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.sites;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;

/**
 * Abstract class for sites with a partial and immediate semantics: evaluate as for a total
 * immediate site (see EvalSite), but if the evaluation returns null, the site remains silent.
 * The site "if" is a good example.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value (possibly null).
 *
 * @author dkitchin
 */
public abstract class PartialSite extends Site {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {

		final Object v = evaluate(args);
		if (v != null) {
			caller.resume(v);
		} else {
			caller.die();
		}

	}

	abstract public Object evaluate(Args args) throws TokenException;

}
