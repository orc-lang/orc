//
// BoundedChannel.java -- Java class BoundedChannel
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;

import scala.collection.JavaConversions;

import orc.CallContext;
import orc.MaterializedCallContext;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.BoundedChannelType;
import orc.run.distrib.AbstractLocation;
import orc.run.distrib.ClusterLocations;
import orc.run.distrib.DOrcPlacementPolicy;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * A bounded channel. With a bound of zero, behaves as a synchronous channel.
 *
 * @author quark
 */
public class BoundedChannel extends EvalSite implements TypedSite {

    @Override
    public Object evaluate(final Args args) throws TokenException {

        if (args.size() == 1) {
            return new ChannelInstance(args.intArg(0));
        } else {
            throw new ArityMismatchException(1, args.size());
        }
    }

    @Override
    public Type orcType() {
        return BoundedChannelType.getBuilder();
    }

    protected class ChannelInstance extends DotSite implements DOrcPlacementPolicy {

        protected final LinkedList<Object> contents;
        protected final LinkedList<MaterializedCallContext> readers;
        protected final LinkedList<MaterializedCallContext> writers;
        protected CallContext closer;
        /** The number of open slots in the channel. */
        protected int open;
        protected boolean closed = false;

        ChannelInstance(final int bound) {
            open = bound;
            contents = new LinkedList<>();
            readers = new LinkedList<MaterializedCallContext>();
            writers = new LinkedList<MaterializedCallContext>();
        }

        @Override
        protected void addMembers() {
            addMember("get", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext reader) {
                    synchronized (ChannelInstance.this) {
                        if (contents.isEmpty()) {
                            if (closed) {
                                reader.halt();
                            } else {
                                reader.setQuiescent();
                                readers.addLast(reader.materialize());
                            }
                        } else {
                            reader.publish(object2value(contents.removeFirst()));
                            if (writers.isEmpty()) {
                                ++open;
                            } else {
                                final CallContext writer = writers.removeFirst();
                                writer.publish(signal());
                            }
                            if (closer != null && contents.isEmpty()) {
                                closer.publish(signal());
                                closer = null;
                            }
                        }
                    }
                }
            });
            addMember("getD", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext reader) {
                    synchronized (ChannelInstance.this) {
                        if (contents.isEmpty()) {
                            reader.halt();
                        } else {
                            reader.publish(object2value(contents.removeFirst()));
                            if (writers.isEmpty()) {
                                ++open;
                            } else {
                                final CallContext writer = writers.removeFirst();
                                writer.publish(signal());
                            }
                            if (closer != null && contents.isEmpty()) {
                                closer.publish(signal());
                                closer = null;
                            }
                        }
                    }
                }

                @Override
                public boolean nonBlocking() {
                    return true;
                }
            });
            addMember("put", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext writer) throws TokenException {
                    synchronized (ChannelInstance.this) {
                        final Object item = args.getArg(0);
                        if (closed) {
                            writer.halt();
                        } else if (!readers.isEmpty()) {
                            final CallContext reader = readers.removeFirst();
                            reader.publish(object2value(item));
                            writer.publish(signal());
                        } else if (open == 0) {
                            contents.addLast(item);
                            writer.setQuiescent();
                            writers.addLast(writer.materialize());
                        } else {
                            contents.addLast(item);
                            --open;
                            writer.publish(signal());
                        }
                    }
                }
            });
            addMember("putD", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext writer) throws TokenException {
                    synchronized (ChannelInstance.this) {
                        final Object item = args.getArg(0);
                        if (closed) {
                            writer.halt();
                        } else if (!readers.isEmpty()) {
                            final CallContext reader = readers.removeFirst();
                            reader.publish(object2value(item));
                            writer.publish(signal());
                        } else if (open == 0) {
                            writer.halt();
                        } else {
                            contents.addLast(item);
                            --open;
                            writer.publish(signal());
                        }
                    }
                }

                @Override
                public boolean nonBlocking() {
                    return true;
                }
            });
            addMember("getAll", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    synchronized (ChannelInstance.this) {
                        // restore open slots
                        open += contents.size() - writers.size();
                        // collect all values in a list
                        final Object out = JavaConversions.collectionAsScalaIterable(contents).toList();
                        contents.clear();

                        ArrayList<CallContext> oldWriters = new ArrayList<>(writers);
                        writers.clear();
                        // resume all writers
                        for (final CallContext writer : oldWriters) {
                            writer.publish(signal());
                        }
                        // notify closer if necessary
                        if (closer != null) {
                            closer.publish(signal());
                            closer = null;
                        }
                        return out;
                    }
                }
            });
            addMember("getOpen", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    return BigInteger.valueOf(open);
                }
            });
            addMember("getBound", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    return BigInteger.valueOf(open + contents.size() - writers.size());
                }
            });
            addMember("isClosed", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    return Boolean.valueOf(closed);
                }
            });
            addMember("close", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext caller) {
                    synchronized (ChannelInstance.this) {
                        closed = true;
                        for (final CallContext reader : readers) {
                            reader.halt();
                        }
                        if (contents.isEmpty()) {
                            caller.publish(signal());
                        } else {
                            closer = caller;
                            closer.setQuiescent();
                        }
                    }
                }
            });
            addMember("closeD", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext caller) {
                    synchronized (ChannelInstance.this) {
                        closed = true;
                        for (final CallContext reader : readers) {
                            reader.halt();
                        }
                        caller.publish(signal());
                    }
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
}
