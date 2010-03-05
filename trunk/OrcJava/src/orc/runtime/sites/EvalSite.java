//
// EvalSite.java -- Java class EvalSite
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
 * Abstract class for sites with a total and immediate semantics: evaluate the arguments and
 * return a value without blocking and without affecting the Orc engine. Essentially, subclasses
 * of this class represent sites without any special concurrent behavior.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value.
 * 
 * @author dkitchin
 */
public abstract class EvalSite extends Site {
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		caller.resume(evaluate(args));
	}

	public abstract Object evaluate(Args args) throws TokenException;
}
