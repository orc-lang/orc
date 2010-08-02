//
// SyncChannel.java -- Java class SyncChannel
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

import orc.error.runtime.TokenException;
//import orc.lib.state.types.SyncChannelType;
import orc.values.sites.compatibility.Args;
import orc.TokenAPI;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Implements the local site SyncChannel, which creates synchronous channels.
 *
 * @author dkitchin
 */
@SuppressWarnings("hiding")
public class SyncChannel extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.values.sites.compatibility.SiteAdaptor#callSite(java.lang.Object[], orc.TokenAPI, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(final Args args) {
		return new SyncChannelInstance();
	}

//	@Override
//	public Type type() throws TypeException {
//		final Type X = new TypeVariable(0);
//		final Type ChannelOfX = new SyncChannelType().instance(X);
//		return new ArrowType(ChannelOfX, 1);
//	}

	private class SenderItem {

		TokenAPI sender;
		Object sent;

		SenderItem(final TokenAPI sender, final Object sent) {
			this.sender = sender;
			this.sent = sent;
		}
	}

	protected class SyncChannelInstance extends DotSite {

		// Invariant: senderQueue is empty or receiverQueue is empty
		protected final LinkedList<SenderItem> senderQueue;
		protected final LinkedList<TokenAPI> receiverQueue;

		SyncChannelInstance() {
			senderQueue = new LinkedList<SenderItem>();
			receiverQueue = new LinkedList<TokenAPI>();
		}

		@Override
		protected void addMembers() {
			addMember("get", new getMethod());
			addMember("put", new putMethod());
		}

		protected class getMethod extends SiteAdaptor {
			@Override
			public void callSite(final Args args, final TokenAPI receiver) {

				// If there are no waiting senders, put this caller on the queue
				if (senderQueue.isEmpty()) {
					//FIXME:receiver.setQuiescent();
					receiverQueue.addLast(receiver);
				}
				// If there is a waiting sender, both sender and receiver return
				else {
					final SenderItem si = senderQueue.removeFirst();
					final TokenAPI sender = si.sender;
					final Object item = si.sent;

					receiver.publish(object2value(item));
					//FIXME:sender.unsetQuiescent();
					sender.publish(signal());
				}

			}
		}

		protected class putMethod extends SiteAdaptor {
			@Override
			public void callSite(final Args args, final TokenAPI sender) throws TokenException {

				final Object item = args.getArg(0);

				// If there are no waiting receivers, put this sender on the queue
				if (receiverQueue.isEmpty()) {
					//FIXME:sender.setQuiescent();
					senderQueue.addLast(new SenderItem(sender, item));
				}

				// If there is a waiting receiver, both receiver and sender return
				else {
					final TokenAPI receiver = receiverQueue.removeFirst();

					//FIXME:receiver.unsetQuiescent();
					receiver.publish(object2value(item));
					sender.publish(signal());
				}

			}
		}

	}

}
