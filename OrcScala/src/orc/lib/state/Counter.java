//
// Counter.java -- Java class Counter
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

import orc.error.runtime.TokenException;
//import orc.lib.state.types.CounterType;
import orc.values.sites.compatibility.Args;
import orc.TokenAPI;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.PartialSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Factory for counters. 
 * @author quark
 */
@SuppressWarnings({"boxing","hiding"})
public class Counter extends EvalSite {
	@Override
	public Object evaluate(final Args args) throws TokenException {
		final int init = args.size() == 0 ? 0 : args.intArg(0);
		return new DotSite() {
			protected int count = init;
			protected final LinkedList<TokenAPI> waiters = new LinkedList<TokenAPI>();

			@Override
			protected void addMembers() {
				addMember("inc", new EvalSite() {
					@Override
					public Object evaluate(final Args args) throws TokenException {
	                  synchronized(Counter.this) {
						++count;
                      }
	                  return signal();
					}
				});
				addMember("dec", new PartialSite() {
					@Override
					public Object evaluate(final Args args) throws TokenException {
                      synchronized(Counter.this) {
						if (count > 0) {
							--count;
							if (count == 0) {
								for (final TokenAPI waiter : waiters) {
									//FIXME:waiter.unsetQuiescent();
									waiter.publish(signal());
								}
								waiters.clear();
							}
							return signal();
						} else {
							return null;
						}
                      }
					}
				});
				addMember("onZero", new SiteAdaptor() {
					@Override
					public void callSite(final Args args, final TokenAPI caller) throws TokenException {
                      synchronized(Counter.this) {
						if (count == 0) {
							caller.publish(signal());
						} else {
							//FIXME:caller.setQuiescent();
							waiters.add(caller);
						}
                      }
					}
				});
				addMember("value", new EvalSite() {
					@Override
					public Object evaluate(final Args args) throws TokenException {
						return count;
					}
				});
			}
		};
	}

}
