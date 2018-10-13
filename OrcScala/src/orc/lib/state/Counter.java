//
// Counter.java -- Java class Counter
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.math.BigDecimal;
import java.util.LinkedList;

import orc.values.sites.compatibility.CallContext;
import orc.MaterializedCallContext;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.CounterType;
import orc.run.distrib.AbstractLocation;
import orc.run.distrib.ClusterLocations;
import orc.run.distrib.DOrcPlacementPolicy;
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
public class Counter extends EvalSite implements TypedSite {
    @Override
    public Object evaluate(final Args args) throws TokenException {
        final int init = args.size() == 0 ? 0 : args.intArg(0);

        if (args.size() > 1) {
            // technically of arity 0 or 1
            throw new ArityMismatchException(1, args.size());
        }

        return new CounterInstance(init);
    }

    @Override
    public Type orcType() {
        return CounterType.getBuilder();
    }

    protected class CounterInstance extends DotSite implements DOrcPlacementPolicy {
        // TODO: Reimplement this without the lock. It will probably scale much better with AtomicInteger
        protected int count;
        protected final LinkedList<MaterializedCallContext> waiters = new LinkedList<MaterializedCallContext>();

        public CounterInstance(final int init) {
            super();
            count = init;
        }

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
                                LinkedList<CallContext> oldWaiters = (LinkedList<CallContext>) waiters.clone();
                                waiters.clear();
                                for (final CallContext waiter : oldWaiters) {
                                    waiter.publish(signal());
                                }
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
                            waiters.add(caller.materialize());
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

        @Override
        public <L extends AbstractLocation> scala.collection.immutable.Set<L> permittedLocations(final ClusterLocations<L> locations) {
            return locations.hereSet();
        }

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
