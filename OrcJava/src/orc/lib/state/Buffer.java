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
import orc.lib.state.types.BufferType;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.ListValue;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;

/**
 * @author cawellington, dkitchin
 *
 * Implements the local site Buffer, which creates buffers (asynchronous channels).
 *
 */
public class Buffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(final Args args) {
		return new BufferInstance();
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type BufferOfX = new BufferType().instance(X);
		return new ArrowType(BufferOfX, 1);
	}

	protected class BufferInstance extends DotSite {

		private final LinkedList<Object> buffer;
		private final LinkedList<Token> readers;
		private Token closer;
		/**
		 * Once this becomes true, no new items may be put,
		 * and gets on an empty buffer die rather than blocking.
		 */
		private boolean closed = false;

		BufferInstance() {
			buffer = new LinkedList<Object>();
			readers = new LinkedList<Token>();
		}

		@Override
		protected void addMembers() {
			addMember("get", new Site() {
				@Override
				public void callSite(final Args args, final Token reader) {
					if (buffer.isEmpty()) {
						if (closed) {
							reader.die();
						} else {
							reader.setQuiescent();
							readers.addLast(reader);
						}
					} else {
						// If there is an item available, pop it and return it.
						reader.resume(buffer.removeFirst());
						if (closer != null && buffer.isEmpty()) {
							closer.unsetQuiescent();
							closer.resume();
							closer = null;
						}
					}
				}
			});
			addMember("put", new Site() {
				@Override
				public void callSite(final Args args, final Token writer) throws TokenException {
					final Object item = args.getArg(0);
					if (closed) {
						writer.die();
						return;
					}
					if (readers.isEmpty()) {
						// If there are no waiting callers, buffer this item.
						buffer.addLast(item);
					} else {
						// If there are callers waiting, give this item to the top caller.
						final Token receiver = readers.removeFirst();
						receiver.unsetQuiescent();
						receiver.resume(item);
					}
					// Since this is an asynchronous buffer, a put call always returns.
					writer.resume();
				}
			});
			addMember("getnb", new Site() {
				@Override
				public void callSite(final Args args, final Token reader) {
					if (buffer.isEmpty()) {
						reader.die();
					} else {
						reader.resume(buffer.removeFirst());
						if (closer != null && buffer.isEmpty()) {
							closer.unsetQuiescent();
							closer.resume();
							closer = null;
						}
					}
				}
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					final Object out = ListValue.make(buffer);
					buffer.clear();
					if (closer != null) {
						closer.unsetQuiescent();
						closer.resume();
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
			addMember("close", new Site() {
				@Override
				public void callSite(final Args args, final Token token) {
					closed = true;
					for (final Token reader : readers) {
						reader.unsetQuiescent();
						reader.die();
					}
					if (buffer.isEmpty()) {
						token.resume();
					} else {
						closer = token;
						closer.setQuiescent();
					}
				}
			});
			addMember("closenb", new Site() {
				@Override
				public void callSite(final Args args, final Token token) {
					closed = true;
					for (final Token reader : readers) {
						reader.unsetQuiescent();
						reader.die();
					}
					token.resume();
				}
			});
		}

		@Override
		public String toString() {
			return super.toString() + buffer.toString();
		}

	}
}
