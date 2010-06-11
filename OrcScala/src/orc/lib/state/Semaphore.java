//
// Semaphore.java -- Java class Semaphore
// Project OrcJava
//
// $Id: Semaphore.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.LinkedList;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
//import orc.lib.state.types.SemaphoreType;
import orc.values.sites.compatibility.Args;
import orc.TokenAPI;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.values.sites.compatibility.type.Type;
import orc.values.sites.compatibility.type.structured.ArrowType;

/**
 * @author quark
 */
@SuppressWarnings("hiding")
public class Semaphore extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new SemaphoreInstance(args.intArg(0));
	}

	@Override
	public Type type() throws TypeException {
		return new ArrowType(Type.INTEGER, null/*FIXME:new SemaphoreType()*/);
	}

	protected class SemaphoreInstance extends DotSite {

		protected final LinkedList<TokenAPI> waiters = new LinkedList<TokenAPI>();
		protected final LinkedList<TokenAPI> snoopers = new LinkedList<TokenAPI>();
		protected int n;

		SemaphoreInstance(final int n) {
			this.n = n;
		}

		@Override
		protected void addMembers() {
			addMember("acquire", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI waiter) {
					if (0 == n) {
						//FIXME:waiter.setQuiescent();
						waiters.addLast(waiter);
						if (!snoopers.isEmpty()) {
							for (final TokenAPI snooper : snoopers) {
								//FIXME:snooper.unsetQuiescent();
								snooper.publish(signal());
							}
							snoopers.clear();
						}
					} else {
						--n;
						waiter.publish(signal());
					}
				}
			});
			addMember("acquirenb", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI waiter) {
					if (0 == n) {
						waiter.halt();
					} else {
						--n;
						waiter.publish(signal());
					}
				}
			});
			addMember("release", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI sender) throws TokenException {
					if (waiters.isEmpty()) {
						++n;
					} else {
						final TokenAPI waiter = waiters.removeFirst();
						//FIXME:waiter.unsetQuiescent();
						waiter.publish(signal());
					}
					sender.publish(signal());
				}
			});
			addMember("snoop", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI snooper) throws TokenException {
					if (waiters.isEmpty()) {
						//FIXME:snooper.setQuiescent();
						snoopers.addLast(snooper);
					} else {
						snooper.publish(signal());
					}
				}
			});
			addMember("snoopnb", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI token) throws TokenException {
					if (waiters.isEmpty()) {
						token.halt();
					} else {
						token.publish(signal());
					}
				}
			});
		}
	}
}
