//
// ThreadedPartialSite.java -- Java class ThreadedPartialSite
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

import kilim.Pausable;
import kilim.Task;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;

/**
 * Abstract class for partial sites whose calls may block (the Java thread). A
 * separate thread is created for every call.
 * 
 * @author quark
 */
public abstract class ThreadedPartialSite extends Site {
	@Override
	public void callSite(final Args args, final Token caller) {
		new Task() {
			@Override
			public void execute() throws Pausable {
				Kilim.runThreaded(new Runnable() {
					public void run() {
						try {
							final Object out = evaluate(args);
							if (out == null) {
								caller.die();
							} else {
								caller.resume(out);
							}
						} catch (final TokenException e) {
							caller.error(e);
						}
					}
				});
			}
		}.start();
	}

	abstract public Object evaluate(Args args) throws TokenException;
}
