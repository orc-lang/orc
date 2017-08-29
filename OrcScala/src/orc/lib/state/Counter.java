//
// Counter.java -- Java class Counter
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.math.BigDecimal;
import java.util.LinkedList;

import orc.CallContext;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.CounterType;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.PartialSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Factory for counters.
 *
 * @author quark
 */
@SuppressWarnings("hiding")
public class Counter extends EvalSite implements TypedSite {
    @Override
    public Object evaluate(final Args args) throws TokenException {
        final int init = args.size() == 0 ? 0 : args.intArg(0);

        if (args.size() > 1) {
            // technically of arity 0 or 1
            throw new ArityMismatchException(1, args.size());
        }

        return new DotSite() {
            // TODO: Reimplement this without the lock. It will probably scale much better with AtomicInteger
            protected int count = init;
            protected final LinkedList<CallContext> waiters = new LinkedList<CallContext>();

            @Override
            protected void addMembers() {
                addMember("inc", new EvalSite() {
                    @Override
                    public Object evaluate(final Args args) throws TokenException {
                        synchronized (Counter.this) {
                            ++count;
                        }
                        return signal();
                    }

                    @Override
                    public boolean nonBlocking() {
                        return true;
                    }
                });
                addMember("dec", new PartialSite() {
                    @Override
                    public Object evaluate(final Args args) throws TokenException {
                        synchronized (Counter.this) {
                            if (count > 0) {
                                --count;
                                if (count == 0) {
                                    for (final CallContext waiter : waiters) {
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

                    @Override
                    public boolean nonBlocking() {
                        return true;
                    }
                });
                addMember("onZero", new SiteAdaptor() {
                    @Override
                    public void callSite(final Args args, final CallContext caller) throws TokenException {
                        synchronized (Counter.this) {
                            if (count == 0) {
                                caller.publish(signal());
                            } else {
                                caller.setQuiescent();
                                waiters.add(caller);
                            }
                        }
                    }
                });
                addMember("value", new EvalSite() {
                    @Override
                    public Object evaluate(final Args args) throws TokenException {
                        return BigDecimal.valueOf(count);
                    }

                    @Override
                    public boolean nonBlocking() {
                        return true;
                    }
                });
            }
        };
    }

    @Override
    public Type orcType() {
        return CounterType.getBuilder();
    }

    @Override
    public boolean nonBlocking() {
        return true;
    }

    @Override
    public int minPublications() {
        return 0;
    }

    @Override
    public int maxPublications() {
        return 1;
    }

    @Override
    public boolean effectFree() {
        return true;
    }
}
