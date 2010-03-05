//
// ThreadSite.java -- Java class ThreadSite
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

package orc.lib.util;

import kilim.Pausable;
import kilim.Task;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;

/**
 * Wrap a site call in a (pooled) thread. This is useful if you have a Java
 * site which may be uncooperative. Currently we do not allow you to create
 * non-pooled threads since we want to place a strict bound on thread resource
 * usage per engine.
 * 
 * @author quark
 */
public class ThreadSite extends EvalSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
		Site thunk;
		try {
			thunk = (Site) args.getArg(0);
		} catch (final ClassCastException e) {
			throw new JavaException(e);
		}
		return makeThreaded(thunk);
	}

	public static Site makeThreaded(final Site site) {
		return new Site() {
			@Override
			public void callSite(final Args args, final Token caller) {
				// Use Kilim.runThreaded to run this in Kilim's
				// thread pool. The task doesn't do anything but
				// wait for a thread to become available
				new Task() {
					@Override
					public void execute() throws Pausable {
						Kilim.runThreaded(new Runnable() {
							public void run() {
								try {
									site.callSite(args, caller);
								} catch (final TokenException e) {
									caller.error(e);
								}
							}
						});
					}
				}.start();
			}
		};
	}
}
