//
// DerivedTracer.java -- Java class DerivedTracer
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
 * Base class for tracers which delegate to something else.
 * Useful to create a tracer which filters (ignores) certain
 * events.
 * 
 * @author quark
 */
public abstract class DerivedTracer extends Tracer {
	private final Tracer tracer;

	public DerivedTracer(final Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public TokenTracer start() {
		return newTokenTracer(tracer.start());
	}

	@Override
	public void finish() {
		tracer.finish();
	}

	protected abstract TokenTracer newTokenTracer(TokenTracer tracer);

	protected abstract class DerivedTokenTracer extends TokenTracer {
		protected TokenTracer tracer;

		public DerivedTokenTracer(final TokenTracer tracer) {
			this.tracer = tracer;
		}

		@Override
		public void block(final PullTrace pull) {
			tracer.block(pull);
		}

		@Override
		public void choke(final StoreTrace store) {
			tracer.choke(store);
		}

		@Override
		public void die() {
			tracer.die();
		}

		@Override
		public void enter(final Closure closure) {
			tracer.enter(closure);
		}

		@Override
		public void leave(final int depth) {
			tracer.leave(depth);
		}

		@Override
		public void error(final TokenException error) {
			tracer.error(error);
		}

		@Override
		public TokenTracer fork() {
			return newTokenTracer(tracer.fork());
		}

		public SourceLocation getSourceLocation() {
			return tracer.getSourceLocation();
		}

		@Override
		public void print(final String value, final boolean newline) {
			tracer.print(value, newline);
		}

		@Override
		public void publish(final Object value) {
			tracer.publish(value);
		}

		@Override
		public PullTrace pull() {
			return tracer.pull();
		}

		@Override
		public void receive(final Object value) {
			tracer.receive(value);
		}

		@Override
		public void send(final Object site, final Object[] arguments) {
			tracer.send(site, arguments);
		}

		public void setSourceLocation(final SourceLocation location) {
			tracer.setSourceLocation(location);
		}

		@Override
		public StoreTrace store(final PullTrace event, final Object value) {
			return tracer.store(event, value);
		}

		@Override
		public void unblock(final StoreTrace store) {
			tracer.unblock(store);
		}
	}
}
