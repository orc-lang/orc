//
// Semaphore.java -- Java class Semaphore
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

package orc.lib.state;

import java.util.LinkedList;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.SemaphoreType;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * @author quark
 */
public class Semaphore extends EvalSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new SemaphoreInstance(args.intArg(0));
	}

	@Override
	public Type type() throws TypeException {
		return new ArrowType(Type.INTEGER, new SemaphoreType());
	}

	protected class SemaphoreInstance extends DotSite {

		private final LinkedList<Token> waiters = new LinkedList<Token>();
		private final LinkedList<Token> snoopers = new LinkedList<Token>();
		private int n;

		SemaphoreInstance(final int n) {
			this.n = n;
		}

		@Override
		protected void addMembers() {
			addMember("acquire", new Site() {
				@Override
				public void callSite(final Args args, final Token waiter) {
					if (0 == n) {
						waiter.setQuiescent();
						waiters.addLast(waiter);
						if (!snoopers.isEmpty()) {
							for (final Token snooper : snoopers) {
								snooper.unsetQuiescent();
								snooper.resume();
							}
							snoopers.clear();
						}
					} else {
						--n;
						waiter.resume();
					}
				}
			});
			addMember("acquirenb", new Site() {
				@Override
				public void callSite(final Args args, final Token waiter) {
					if (0 == n) {
						waiter.die();
					} else {
						--n;
						waiter.resume();
					}
				}
			});
			addMember("release", new Site() {
				@Override
				public void callSite(final Args args, final Token sender) throws TokenException {
					if (waiters.isEmpty()) {
						++n;
					} else {
						final Token waiter = waiters.removeFirst();
						waiter.unsetQuiescent();
						waiter.resume();
					}
					sender.resume();
				}
			});
			addMember("snoop", new Site() {
				@Override
				public void callSite(final Args args, final Token snooper) throws TokenException {
					if (waiters.isEmpty()) {
						snooper.setQuiescent();
						snoopers.addLast(snooper);
					} else {
						snooper.resume();
					}
				}
			});
			addMember("snoopnb", new Site() {
				@Override
				public void callSite(final Args args, final Token token) throws TokenException {
					if (waiters.isEmpty()) {
						token.die();
					} else {
						token.resume();
					}
				}
			});
		}
	}
}
