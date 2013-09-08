//
// BoundedChannel.java -- Java class BoundedChannel
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

import java.math.BigInteger;
import java.util.LinkedList;

import orc.Handle;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.BoundedChannelType;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

import scala.collection.JavaConversions;

/**
 * A bounded cchannel.
 * With a bound of zero, behaves as a synchronous channel.
 *
 * @author quark
 */
public class BoundedChannel extends EvalSite implements TypedSite {

	/* (non-Javadoc)
	 * @see orc.values.sites.compatibility.SiteAdaptor#callSite(java.lang.Object[], orc.Handle, orc.runtime.values.GroupCell, orc.OrcRuntime)
	 */
	@Override
	public Object evaluate(final Args args) throws TokenException {

	  if (args.size() == 1) {
	    return new ChannelInstance(args.intArg(0));
	  }
	  else {
	    throw new ArityMismatchException(1, args.size());
	  }
	}

	@Override
	public Type orcType() {
		return BoundedChannelType.getBuilder();
	}

	protected class ChannelInstance extends DotSite {

		protected final LinkedList<Object> Channel;
		protected final LinkedList<Handle> readers;
		protected final LinkedList<Handle> writers;
		protected Handle closer;
		/** The number of open slots in the channel. */
		protected int open;
		protected boolean closed = false;

		ChannelInstance(final int bound) {
			open = bound;
			Channel = new LinkedList<Object>();
			readers = new LinkedList<Handle>();
			writers = new LinkedList<Handle>();
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
							reader.publish(object2value(Channel.removeFirst()));
							if (writers.isEmpty()) {
								++open;
							} else {
								final Handle writer = writers.removeFirst();
								writer.publish(signal());
							}
							if (closer != null && Channel.isEmpty()) {
								closer.publish(signal());
								closer = null;
							}
						}
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
							if (writers.isEmpty()) {
								++open;
							} else {
								final Handle writer = writers.removeFirst();
								writer.publish(signal());
							}
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
						} else if (!readers.isEmpty()) {
							final Handle reader = readers.removeFirst();
							reader.publish(object2value(item));
							writer.publish(signal());
						} else if (open == 0) {
							Channel.addLast(item);
							writers.addLast(writer);
						} else {
							Channel.addLast(item);
							--open;
							writer.publish(signal());
						}
					}
				}
			});
			addMember("putD", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle writer) throws TokenException {
					synchronized (ChannelInstance.this) {
						final Object item = args.getArg(0);
						if (closed) {
							writer.halt();
						} else if (!readers.isEmpty()) {
							final Handle reader = readers.removeFirst();
							reader.publish(object2value(item));
							writer.publish(signal());
						} else if (open == 0) {
							writer.halt();
						} else {
							Channel.addLast(item);
							--open;
							writer.publish(signal());
						}
					}
				}
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					synchronized (ChannelInstance.this) {
						// restore open slots
						open += Channel.size() - writers.size();
						// collect all values in a list
						final Object out = JavaConversions.collectionAsScalaIterable(Channel).toList();
						Channel.clear();
						// resume all writers
						for (final Handle writer : writers) {
							writer.publish(signal());
						}
						writers.clear();
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
					return BigInteger.valueOf(open + Channel.size() - writers.size());
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
	}
}
