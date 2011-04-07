//
// Channel.java -- Java class Channel
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

import orc.Handle;
import orc.error.runtime.TokenException;
import orc.lib.state.types.ChannelType;
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

	/* (non-Javadoc)
	 * @see orc.values.sites.compatibility.SiteAdaptor#callSite(java.lang.Object[], orc.Handle, orc.runtime.values.GroupCell, orc.OrcRuntime)
	 */
	@Override
	public Object evaluate(final Args args) {
		return new ChannelInstance();
	}

	@Override
	public Type orcType() {
		return ChannelType.getBuilder();
	}

	//	@Override
	//	public Type type() throws TypeException {
	//		final Type X = new TypeVariable(0);
	//		final Type ChannelOfX = new ChannelType().instance(X);
	//		return new ArrowType(ChannelOfX, 1);
	//	}

	protected class ChannelInstance extends DotSite {

		protected final LinkedList<Object> Channel;
		protected final LinkedList<Handle> readers;
		protected Handle closer;
		/**
		 * Once this becomes true, no new items may be put,
		 * and gets on an empty channel die rather than blocking.
		 */
		protected boolean closed = false;

		ChannelInstance() {
			Channel = new LinkedList<Object>();
			readers = new LinkedList<Handle>();
		}

		@Override
		protected void addMembers() {
			addMember("get", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle reader) {
					synchronized (ChannelInstance.this) {
						if (Channel.isEmpty()) {
							if (closed) {
								reader.halt();
							} else {
								readers.addLast(reader);
							}
						} else {
							// If there is an item available, pop it and return it.
							reader.publish(object2value(Channel.removeFirst()));
							if (closer != null && Channel.isEmpty()) {
								closer.publish(signal());
								closer = null;
							}
						}
					}
				}
			});
			addMember("put", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle writer) throws TokenException {
					synchronized (ChannelInstance.this) {
						final Object item = args.getArg(0);
						if (closed) {
							writer.halt();
							return;
						}
						if (readers.isEmpty()) {
							// If there are no waiting callers, queue this item.
							Channel.addLast(item);
						} else {
							// If there are callers waiting, give this item to the top caller.
							final Handle receiver = readers.removeFirst();
							receiver.publish(object2value(item));
						}
						// Since this is an asynchronous channel, a put call always returns.
						writer.publish(signal());
					}
				}
			});
			addMember("getD", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle reader) {
					synchronized (ChannelInstance.this) {
						if (Channel.isEmpty()) {
							reader.halt();
						} else {
							reader.publish(object2value(Channel.removeFirst()));
							if (closer != null && Channel.isEmpty()) {
								closer.publish(signal());
								closer = null;
							}
						}
					}
				}
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					synchronized (ChannelInstance.this) {
						final Object out = scala.collection.JavaConversions.asIterable(Channel).toList();
						Channel.clear();
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
			});
			addMember("close", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle token) {
					synchronized (ChannelInstance.this) {
						closed = true;
						for (final Handle reader : readers) {
							reader.halt();
						}
						if (Channel.isEmpty()) {
							token.publish(signal());
						} else {
							closer = token;
						}
					}
				}
			});
			addMember("closeD", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle token) {
					synchronized (ChannelInstance.this) {
						closed = true;
						for (final Handle reader : readers) {
							reader.halt();
						}
						token.publish(signal());
					}
				}
			});
		}

		@Override
		public String toString() {
			return super.toString() + Channel.toString();
		}

	}
}
