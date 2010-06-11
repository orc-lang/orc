//
// Buffer.java -- Java class Buffer
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

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
//import orc.lib.state.types.BufferType;
import orc.values.sites.compatibility.Args;
import orc.TokenAPI;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;
import orc.values.sites.compatibility.type.Type;
import orc.values.sites.compatibility.type.TypeVariable;
import orc.values.sites.compatibility.type.structured.ArrowType;

/**
 * Implements the local site Buffer, which creates buffers (asynchronous channels).
 *
 * @author cawellington, dkitchin
 */
@SuppressWarnings("boxing")
public class Buffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.values.sites.compatibility.SiteAdaptor#callSite(java.lang.Object[], orc.TokenAPI, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(final Args args) {
		return new BufferInstance();
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type BufferOfX = null;//FIXME:new BufferType().instance(X);
		return new ArrowType(BufferOfX, 1);
	}

	protected class BufferInstance extends DotSite {

		protected final LinkedList<Object> buffer;
		protected final LinkedList<TokenAPI> readers;
		protected TokenAPI closer;
		/**
		 * Once this becomes true, no new items may be put,
		 * and gets on an empty buffer die rather than blocking.
		 */
		protected boolean closed = false;

		BufferInstance() {
			buffer = new LinkedList<Object>();
			readers = new LinkedList<TokenAPI>();
		}

		@Override
		protected void addMembers() {
			addMember("get", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI reader) {
					if (buffer.isEmpty()) {
						if (closed) {
							reader.halt();
						} else {
							//FIXME:reader.setQuiescent();
							readers.addLast(reader);
						}
					} else {
						// If there is an item available, pop it and return it.
						reader.publish(object2value(buffer.removeFirst()));
						if (closer != null && buffer.isEmpty()) {
							//FIXME:closer.unsetQuiescent();
							closer.publish(signal());
							closer = null;
						}
					}
				}
			});
			addMember("put", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI writer) throws TokenException {
					final Object item = args.getArg(0);
					if (closed) {
						writer.halt();
						return;
					}
					if (readers.isEmpty()) {
						// If there are no waiting callers, buffer this item.
						buffer.addLast(item);
					} else {
						// If there are callers waiting, give this item to the top caller.
						final TokenAPI receiver = readers.removeFirst();
						//FIXME:receiver.unsetQuiescent();
						receiver.publish(object2value(item));
					}
					// Since this is an asynchronous buffer, a put call always returns.
					writer.publish(signal());
				}
			});
			addMember("getnb", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI reader) {
					if (buffer.isEmpty()) {
						reader.halt();
					} else {
						reader.publish(object2value(buffer.removeFirst()));
						if (closer != null && buffer.isEmpty()) {
							//FIXME:closer.unsetQuiescent();
							closer.publish(signal());
							closer = null;
						}
					}
				}
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					final Object out = null;//OrcList(buffer);
					buffer.clear();
					if (closer != null) {
						//FIXME:closer.unsetQuiescent();
						closer.publish(signal());
						closer = null;
					}
					return out;
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
			});
			addMember("closenb", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final TokenAPI token) {
					closed = true;
					for (final TokenAPI reader : readers) {
						//FIXME:reader.unsetQuiescent();
						reader.halt();
					}
					token.publish(signal());
				}
			});
		}

		@Override
		public String toString() {
			return super.toString() + buffer.toString();
		}

	}
}
