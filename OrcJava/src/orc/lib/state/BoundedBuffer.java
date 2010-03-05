//
// BoundedBuffer.java -- Java class BoundedBuffer
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
import orc.lib.state.types.BoundedBufferType;
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
 * A bounded buffer.
 * With a bound of zero, behaves as a synchronous channel.
 * 
 * @author quark
 */
public class BoundedBuffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(final Args args) throws TokenException {
		return new BufferInstance(args.intArg(0));
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type BufferOfX = new BoundedBufferType().instance(X);
		return new ArrowType(Type.INTEGER, BufferOfX, 1);
	}

	protected class BufferInstance extends DotSite {

		private final LinkedList<Object> buffer;
		private final LinkedList<Token> readers;
		private final LinkedList<Token> writers;
		private Token closer;
		/** The number of open slots in the buffer. */
		private int open;
		private boolean closed = false;

		BufferInstance(final int bound) {
			open = bound;
			buffer = new LinkedList<Object>();
			readers = new LinkedList<Token>();
			writers = new LinkedList<Token>();
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
						reader.resume(buffer.removeFirst());
						if (writers.isEmpty()) {
							++open;
						} else {
							final Token writer = writers.removeFirst();
							writer.unsetQuiescent();
							writer.resume();
						}
						if (closer != null && buffer.isEmpty()) {
							closer.unsetQuiescent();
							closer.resume();
							closer = null;
						}
					}
				}
			});
			addMember("getnb", new Site() {
				@Override
				public void callSite(final Args args, final Token reader) {
					if (buffer.isEmpty()) {
						reader.die();
					} else {
						reader.resume(buffer.removeFirst());
						if (writers.isEmpty()) {
							++open;
						} else {
							final Token writer = writers.removeFirst();
							writer.unsetQuiescent();
							writer.resume();
						}
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
					} else if (!readers.isEmpty()) {
						final Token reader = readers.removeFirst();
						reader.unsetQuiescent();
						reader.resume(item);
						writer.resume();
					} else if (open == 0) {
						buffer.addLast(item);
						writer.setQuiescent();
						writers.addLast(writer);
					} else {
						buffer.addLast(item);
						--open;
						writer.resume();
					}
				}
			});
			addMember("putnb", new Site() {
				@Override
				public void callSite(final Args args, final Token writer) throws TokenException {
					final Object item = args.getArg(0);
					if (closed) {
						writer.die();
					} else if (!readers.isEmpty()) {
						final Token reader = readers.removeFirst();
						reader.unsetQuiescent();
						reader.resume(item);
						writer.resume();
					} else if (open == 0) {
						writer.die();
					} else {
						buffer.addLast(item);
						--open;
						writer.resume();
					}
				}
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					// restore open slots
					open += buffer.size() - writers.size();
					// collect all values in a list
					final Object out = ListValue.make(buffer);
					buffer.clear();
					// resume all writers
					for (final Token writer : writers) {
						writer.unsetQuiescent();
						writer.resume();
					}
					writers.clear();
					// notify closer if necessary
					if (closer != null) {
						closer.unsetQuiescent();
						closer.resume();
						closer = null;
					}
					return out;
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
	}
}
