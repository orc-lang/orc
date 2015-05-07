//
// ThreadedSite.java -- Java class ThreadedSite
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

import java.util.concurrent.Callable;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;

/**
 * Abstract class for sites whose calls may block (the Java thread).
 * @author quark
 */
public abstract class ThreadedSite extends Site {
	@Override
	public void callSite(final Args args, final Token caller) {
		Kilim.runThreaded(caller, new Callable<Object>() {
			public Object call() throws Exception {
				return evaluate(args);
			}
		});
	}

	abstract public Object evaluate(Args args) throws TokenException;
}
