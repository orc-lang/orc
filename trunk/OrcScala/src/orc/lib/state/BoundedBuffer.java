//
// BoundedBuffer.java -- Java class BoundedBuffer
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

import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.Handle;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.lib.state.types.BoundedBufferType;
import orc.values.sites.TypedSite;
import orc.types.Type;


/**
 * A bounded buffer.
 * With a bound of zero, behaves as a synchronous channel.
 * 
 * @author quark
 */
public class BoundedBuffer extends EvalSite implements TypedSite {

	/* (non-Javadoc)
	 * @see orc.values.sites.compatibility.SiteAdaptor#callSite(java.lang.Object[], orc.Handle, orc.runtime.values.GroupCell, orc.OrcRuntime)
	 */
	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new BufferInstance(args.intArg(0));
	}

	@Override
    public Type orcType() {
      return BoundedBufferType.getBuilder();
    }

	protected class BufferInstance extends DotSite {

		protected final LinkedList<Object> buffer;
		protected final LinkedList<Handle> readers;
		protected final LinkedList<Handle> writers;
		protected Handle closer;
		/** The number of open slots in the buffer. */
		protected int open;
		protected boolean closed = false;

		BufferInstance(final int bound) {
			open = bound;
			buffer = new LinkedList<Object>();
			readers = new LinkedList<Handle>();
			writers = new LinkedList<Handle>();
		}

		@Override
		protected void addMembers() {
			addMember("get", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle reader) {
                  synchronized(BufferInstance.this) {
					if (buffer.isEmpty()) {
						if (closed) {
							reader.halt();
						} else {
							//FIXME:reader.setQuiescent();
							readers.addLast(reader);
						}
					} else {
						reader.publish(object2value(buffer.removeFirst()));
						if (writers.isEmpty()) {
							++open;
						} else {
							final Handle writer = writers.removeFirst();
							//FIXME:writer.unsetQuiescent();
							writer.publish(signal());
						}
						if (closer != null && buffer.isEmpty()) {
							//FIXME:closer.unsetQuiescent();
							closer.publish(signal());
							closer = null;
						}
					}
                  }
				}
			});
			addMember("getnb", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle reader) {
                  synchronized(BufferInstance.this) {
					if (buffer.isEmpty()) {
						reader.halt();
					} else {
						reader.publish(object2value(buffer.removeFirst()));
						if (writers.isEmpty()) {
							++open;
						} else {
							final Handle writer = writers.removeFirst();
							//FIXME:writer.unsetQuiescent();
							writer.publish(signal());
						}
						if (closer != null && buffer.isEmpty()) {
							//FIXME:closer.unsetQuiescent();
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
                  synchronized(BufferInstance.this) {
                    final Object item = args.getArg(0);
					if (closed) {
						writer.halt();
					} else if (!readers.isEmpty()) {
						final Handle reader = readers.removeFirst();
						//FIXME:reader.unsetQuiescent();
						reader.publish(object2value(item));
						writer.publish(signal());
					} else if (open == 0) {
						buffer.addLast(item);
						//FIXME:writer.setQuiescent();
						writers.addLast(writer);
					} else {
						buffer.addLast(item);
						--open;
						writer.publish(signal());
					}
                  }
				}
			});
			addMember("putnb", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle writer) throws TokenException {
                  synchronized(BufferInstance.this) {
					final Object item = args.getArg(0);
					if (closed) {
						writer.halt();
					} else if (!readers.isEmpty()) {
						final Handle reader = readers.removeFirst();
						//FIXME:reader.unsetQuiescent();
						reader.publish(object2value(item));
						writer.publish(signal());
					} else if (open == 0) {
						writer.halt();
					} else {
						buffer.addLast(item);
						--open;
						writer.publish(signal());
					}
                  }
				}
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
                  synchronized(BufferInstance.this) {
					// restore open slots
					open += buffer.size() - writers.size();
					// collect all values in a list
					final Object out = buffer.clone();
					buffer.clear();
					// resume all writers
					for (final Handle writer : writers) {
						//FIXME:writer.unsetQuiescent();
						writer.publish(signal());
					}
					writers.clear();
					// notify closer if necessary
					if (closer != null) {
						//closer.unsetQuiescent();
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
					return BigInteger.valueOf(open + buffer.size() - writers.size());
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
                  synchronized(BufferInstance.this) {
					closed = true;
					for (final Handle reader : readers) {
						//FIXME:reader.unsetQuiescent();
						reader.halt();
					}
					if (buffer.isEmpty()) {
						token.publish(signal());
					} else {
						closer = token;
						//FIXME:closer.setQuiescent();
					}
                  }
				}
			});
			addMember("closenb", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle token) {
                  synchronized(BufferInstance.this) {
					closed = true;
					for (final Handle reader : readers) {
						//FIXME:reader.unsetQuiescent();
						reader.halt();
					}
					token.publish(signal());
                  }
				}
			});
		}
	}
}
