//
// Channel.java -- Java class Channel
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.LinkedList;

import orc.Handle;
import orc.error.runtime.ArityMismatchException;
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

	protected class ChannelInstance extends DotSite {

		protected final LinkedList<Object> contents;
		protected final LinkedList<Handle> readers;
		protected Handle closer;
		/**
		 * Once this becomes true, no new items may be put, and gets on an empty
		 * channel die rather than blocking.
		 */
		protected boolean closed = false;

		ChannelInstance() {
			contents = new LinkedList<Object>();
			readers = new LinkedList<Handle>();
		}

		@Override
		protected void addMembers() {
			addMember("get", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle reader) {
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
							contents.addLast(item);
						} else {
							// If there are callers waiting, give this item to
							// the top caller.
							final Handle receiver = readers.removeFirst();
							receiver.publish(object2value(item));
						}
						// Since this is an asynchronous channel, a put call
						// always returns.
						writer.publish(signal());
					}
				}
			});
			addMember("getD", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle reader) {
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
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					synchronized (ChannelInstance.this) {
						final Object out = scala.collection.JavaConversions.collectionAsScalaIterable(contents).toList();
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
			});
			addMember("close", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle caller) {
					synchronized (ChannelInstance.this) {
						closed = true;
						for (final Handle reader : readers) {
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
				public void callSite(final Args args, final Handle caller) {
					synchronized (ChannelInstance.this) {
						closed = true;
						for (final Handle reader : readers) {
							reader.halt();
						}
						caller.publish(signal());
					}
				}
			});
		}

		@Override
		public String toString() {
			return super.toString() + contents.toString();
		}

	}

    @Override
    public boolean nonBlocking() { return true; }
    @Override
    public int minPublications() { return 1; }
    @Override
    public int maxPublications() { return 1; }
    @Override
    public boolean effectFree() { return true; }
}
