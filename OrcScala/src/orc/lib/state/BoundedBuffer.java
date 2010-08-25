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

import java.util.LinkedList;

import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.TokenAPI;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * A bounded buffer.
 * With a bound of zero, behaves as a synchronous channel.
 * 
 * @author quark
 */
@SuppressWarnings("boxing")
public class BoundedBuffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.values.sites.compatibility.SiteAdaptor#callSite(java.lang.Object[], orc.TokenAPI, orc.runtime.values.GroupCell, orc.OrcRuntime)
	 */
	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new BufferInstance(args.intArg(0));
	}

//	@Override
//	public Type type() throws TypeException {
//		final Type X = new TypeVariable(0);
//		final Type BufferOfX = new BoundedBufferType().instance(X);
//		return new ArrowType(Type.INTEGER, BufferOfX, 1);
//	}

	protected class BufferInstance extends DotSite {

		protected final LinkedList<Object> buffer;
		protected final LinkedList<TokenAPI> readers;
		protected final LinkedList<TokenAPI> writers;
		protected TokenAPI closer;
		/** The number of open slots in the buffer. */
		protected int open;
		protected boolean closed = false;

		BufferInstance(final int bound) {
			open = bound;
			buffer = new LinkedList<Object>();
			readers = new LinkedList<TokenAPI>();
			writers = new LinkedList<TokenAPI>();
		}

		@Override
		protected void addMembers() {
			addMember("get", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI reader) {
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
							final TokenAPI writer = writers.removeFirst();
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
				public void callSite(final Args args, final TokenAPI reader) {
                  synchronized(BufferInstance.this) {
					if (buffer.isEmpty()) {
						reader.halt();
					} else {
						reader.publish(object2value(buffer.removeFirst()));
						if (writers.isEmpty()) {
							++open;
						} else {
							final TokenAPI writer = writers.removeFirst();
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
				public void callSite(final Args args, final TokenAPI writer) throws TokenException {
                  synchronized(BufferInstance.this) {
                    final Object item = args.getArg(0);
					if (closed) {
						writer.halt();
					} else if (!readers.isEmpty()) {
						final TokenAPI reader = readers.removeFirst();
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
				public void callSite(final Args args, final TokenAPI writer) throws TokenException {
                  synchronized(BufferInstance.this) {
					final Object item = args.getArg(0);
					if (closed) {
						writer.halt();
					} else if (!readers.isEmpty()) {
						final TokenAPI reader = readers.removeFirst();
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
					for (final TokenAPI writer : writers) {
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
					return open;
				}
			});
			addMember("getBound", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					return open + buffer.size() - writers.size();
				}
			});
			addMember("isClosed", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					return closed;
				}
			});
			addMember("close", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI token) {
                  synchronized(BufferInstance.this) {
					closed = true;
					for (final TokenAPI reader : readers) {
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
				public void callSite(final Args args, final TokenAPI token) {
                  synchronized(BufferInstance.this) {
					closed = true;
					for (final TokenAPI reader : readers) {
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
