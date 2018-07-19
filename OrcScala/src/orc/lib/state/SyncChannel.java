//
// SyncChannel.java -- Java class SyncChannel
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.LinkedList;

import orc.CallContext;
import orc.MaterializedCallContext;
import orc.error.runtime.TokenException;
import orc.lib.state.types.SyncChannelType;
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
 * Implements the local site SyncChannel, which creates synchronous channels.
 *
 * @author dkitchin
 */
@SuppressWarnings("hiding")
public class SyncChannel extends EvalSite implements TypedSite {

    @Override
    public Object evaluate(final Args args) {
        return new SyncChannelInstance();
    }

    @Override
    public Type orcType() {
        return SyncChannelType.getBuilder();
    }

    private class SenderItem {

        MaterializedCallContext sender;
        Object sent;

        SenderItem(final MaterializedCallContext sender, final Object sent) {
            this.sender = sender;
            this.sent = sent;
        }
    }

    protected class SyncChannelInstance extends DotSite implements DOrcPlacementPolicy {

        // Invariant: senderQueue is empty or receiverQueue is empty
        protected final LinkedList<SenderItem> senderQueue;
        protected final LinkedList<MaterializedCallContext> receiverQueue;

        SyncChannelInstance() {
            senderQueue = new LinkedList<>();
            receiverQueue = new LinkedList<MaterializedCallContext>();
        }

        @Override
        protected void addMembers() {
            addMember("get", new getMethod());
            addMember("put", new putMethod());
        }

        protected class getMethod extends SiteAdaptor {
            @Override
            public void callSite(final Args args, final CallContext receiver) {

                // If there are no waiting senders, put this caller on the queue
                if (senderQueue.isEmpty()) {
                    receiver.setQuiescent();
                    receiverQueue.addLast(receiver.materialize());
                }
                // If there is a waiting sender, both sender and receiver return
                else {
                    final SenderItem si = senderQueue.removeFirst();
                    final CallContext sender = si.sender;
                    final Object item = si.sent;

                    receiver.publish(object2value(item));
                    sender.publish(signal());
                }

            }
        }

        protected class putMethod extends SiteAdaptor {
            @Override
            public void callSite(final Args args, final CallContext sender) throws TokenException {

                final Object item = args.getArg(0);

                // If there are no waiting receivers, put this sender on the
                // queue
                if (receiverQueue.isEmpty()) {
                    sender.setQuiescent();
                    senderQueue.addLast(new SenderItem(sender.materialize(), item));
                }

                // If there is a waiting receiver, both receiver and sender
                // return
                else {
                    final CallContext receiver = receiverQueue.removeFirst();

                    receiver.publish(object2value(item));
                    sender.publish(signal());
                }

            }
        }

        @Override
        public <L extends AbstractLocation> scala.collection.immutable.Set<L> permittedLocations(final ClusterLocations<L> locations) {
            return locations.hereSet();
        }

    }

}
