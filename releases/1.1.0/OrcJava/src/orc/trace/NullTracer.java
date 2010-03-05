//
// NullTracer.java -- Java class NullTracer
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.runtime.values.Closure;

/**
 * Do-nothing tracer, used when tracing is not enabled.
 * @author quark
 */
public class NullTracer extends Tracer {
	@Override
	public TokenTracer start() {
		return TOKEN_TRACER;
	}

	@Override
	public void finish() {
	}

	private static final TokenTracer TOKEN_TRACER = new TokenTracer() {
		@Override
		public void send(final Object site, final Object[] arguments) {
		}

		@Override
		public void die() {
		}

		@Override
		public void choke(final StoreTrace store) {
		}

		@Override
		public void receive(final Object value) {
		}

		@Override
		public void unblock(final StoreTrace store) {
		}

		@Override
		public TokenTracer fork() {
			return this;
		}

		@Override
		public void enter(final Closure closure) {
		}

		@Override
		public void leave(final int depth) {
		}

		@Override
		public void print(final String value, final boolean newline) {
		}

		@Override
		public void publish(final Object value) {
		}

		@Override
		public void error(final TokenException error) {
		}

		public void setSourceLocation(final SourceLocation location) {
		}

		public SourceLocation getSourceLocation() {
			return null;
		}

		@Override
		public void block(final PullTrace pull) {
		}

		@Override
		public PullTrace pull() {
			return null;
		}

		@Override
		public StoreTrace store(final PullTrace event, final Object value) {
			return null;
		}
	};
}
