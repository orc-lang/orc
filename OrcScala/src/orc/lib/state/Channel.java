//
// Channel.java -- Java class Channel
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.ArrayList;
import java.util.LinkedList;

import orc.CallContext;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.ChannelType;
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
 * Implements the local site Channel, which creates asynchronous channels.
 *
 * @author cawellington, dkitchin
 */
public class Channel extends EvalSite implements TypedSite {

    @Override
    public Object evaluate(final Args args) throws TokenException {
        if (args.size() == 0) {
            return new ChannelInstance();
        } else {
            throw new ArityMismatchException(0, args.size());
        }
    }

    @Override
    public Type orcType() {
        return ChannelType.getBuilder();
    }

    // @Override
    // public Type type() throws TypeException {
    // final Type X = new TypeVariable(0);
    // final Type ChannelOfX = new ChannelType().instance(X);
    // return new ArrowType(ChannelOfX, 1);
    // }

    protected class ChannelInstance extends DotSite implements DOrcPlacementPolicy {

        protected final LinkedList<Object> contents;
        protected final LinkedList<CallContext> readers;
        protected CallContext closer;
        /**
         * Once this becomes true, no new items may be put, and gets on an empty
         * channel die rather than blocking.
         */
        protected boolean closed = false;

        ChannelInstance() {
            contents = new LinkedList<>();
            readers = new LinkedList<CallContext>();
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
                                readers.addLast(reader);
                            }
                        } else {
                            // If there is an item available, pop it and return
                            // it.
                            reader.publish(object2value(contents.removeFirst()));
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
                            return;
                        }
                        while(true) { // Contains break. Loops until a live reader is removed or readers is empty.
                          if (readers.isEmpty()) {
                              // If there are no waiting callers, queue this item.
                              contents.addLast(item);
                              break;
                          } else {
                              // If there are callers waiting, give this item to
                              // the top caller.
                              CallContext receiver = readers.removeFirst();
                              if (receiver.isLive()) { // If the reader is live then publish into it.
                                receiver.publish(object2value(item));
                                break;
                              } else { // If the reader is dead then go through the loop again to get another reader.
                              }
                          }
                        }
                        // Since this is an asynchronous channel, a put call
                        // always returns.
                        writer.publish(signal());
                    }
                }

                @Override
                public boolean nonBlocking() {
                    return true;
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
            addMember("getAll", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    synchronized (ChannelInstance.this) {
                        final ArrayList<Object> convertedContents = new ArrayList<>(contents.size());
                        for (final Object o : contents) {
                            convertedContents.add(object2value(o));
                        }
                        final Object out = scala.collection.JavaConversions.collectionAsScalaIterable(convertedContents).toList();
                        contents.clear();
                        if (closer != null) {
                            closer.publish(signal());
                            closer = null;
                        }
                        return out;
                    }
                }
            });
            addMember("isClosed", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    return Boolean.valueOf(closed);
                }

                @Override
                public boolean nonBlocking() {
                    return true;
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

                @Override
                public boolean nonBlocking() {
                    return true;
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
        public String toString() {
            synchronized (ChannelInstance.this) {
                return super.toString() + contents.toString();
            }
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
