//
// Semaphore.java -- Java class Semaphore
// Project OrcScala
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
import java.util.Queue;

import orc.Handle;
import orc.error.runtime.TokenException;
import orc.lib.state.types.SemaphoreType;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * @authors quark, dkitchin
 */
@SuppressWarnings("hiding")
public class Semaphore extends EvalSite implements TypedSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {
	  int initialValue = args.intArg(0);	
	  if (initialValue >= 0) {
	    return new SemaphoreInstance(initialValue);
	  }
	  else {
	    throw new IllegalArgumentException("Semaphore requires a non-negative argument");
	  }
	}

	@Override
	public Type orcType() {
		return SemaphoreType.getBuilder();
	}

	protected class SemaphoreInstance extends DotSite {

		protected final Queue<Handle> waiters = new LinkedList<Handle>();
		protected final Queue<Handle> snoopers = new LinkedList<Handle>();
		
		/* Invariant: n >= 0 */
		protected int n;

		/* Precondition: n >= 0 */
		SemaphoreInstance(final int n) {
			this.n = n;
		}

		@Override
		protected void addMembers() {
			addMember("acquire", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle waiter) {
					synchronized (SemaphoreInstance.this) {
						if (0 == n) {
							waiters.offer(waiter);
							if (!snoopers.isEmpty()) {
								for (final Handle snooper : snoopers) {
									snooper.publish(signal());
								}
								snoopers.clear();
							}
						} else {
							--n;
							waiter.publish(signal());
						}
					}
				}
			});
			addMember("acquireD", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle waiter) {
					synchronized (SemaphoreInstance.this) {
						if (0 == n) {
							waiter.halt();
						} else {
							--n;
							waiter.publish(signal());
						}
					}
				}
			});
			addMember("release", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle sender) throws TokenException {
					synchronized (SemaphoreInstance.this) {
						if (waiters.isEmpty()) {
							++n;
						} else {
							final Handle waiter = waiters.poll();
							waiter.publish(signal());
						}
						sender.publish(signal());
					}
				}
			});
			addMember("snoop", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle snooper) throws TokenException {
					synchronized (SemaphoreInstance.this) {
						if (waiters.isEmpty()) {
							snoopers.offer(snooper);
						} else {
							snooper.publish(signal());
						}
					}
				}
			});
			addMember("snoopD", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle token) throws TokenException {
					synchronized (SemaphoreInstance.this) {
						if (waiters.isEmpty()) {
							token.halt();
						} else {
							token.publish(signal());
						}
					}
				}
			});
		}
	}
}
