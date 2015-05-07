//
// MinimizeTracer.java -- Java class MinimizeTracer
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
 * Wrap a tracer to ignore all but the events essential to reconstruct
 * the trace using the same (deterministic) engine. The necessary events are:
 * <ul>
 * <li>fork: to match the thread structure
 * <li>receive: to record the timing and value of site responses
 * <li>die: to record tokens killed during site calls
 * <li>error: to record token errors during site calls
 * </ul>
 * 
 * @author quark
 */
public class MinimizeTracer extends DerivedTracer {
	public MinimizeTracer(final Tracer tracer) {
		super(tracer);
	}

	@Override
	protected TokenTracer newTokenTracer(final TokenTracer tracer) {
		return new MinimizeTokenTracer(tracer);
	}

	private class MinimizeTokenTracer extends DerivedTokenTracer {
		public MinimizeTokenTracer(final TokenTracer tracer) {
			super(tracer);
		}

		private boolean inSend = false;

		@Override
		public void enter(final Closure closure) {
		}

		@Override
		public void leave(final int depth) {
		}

		@Override
		public void block(final PullTrace pull) {
		}

		@Override
		public void choke(final StoreTrace store) {
		}

		@Override
		public void print(final String value, final boolean newline) {
		}

		@Override
		public void publish(final Object value) {
		}

		@Override
		public PullTrace pull() {
			return null;
		}

		@Override
		public void setSourceLocation(final SourceLocation location) {
		}

		@Override
		public StoreTrace store(final PullTrace event, final Object value) {
			return null;
		}

		@Override
		public void unblock(final StoreTrace store) {
		}

		@Override
		public void send(final Object site, final Object[] arguments) {
			inSend = true;
		}

		@Override
		public void receive(final Object value) {
			super.receive(value);
			inSend = false;
		}

		@Override
		public void die() {
			if (inSend) {
				super.die();
			}
		}

		@Override
		public void error(final TokenException error) {
			if (inSend) {
				super.error(error);
			}
		}

		@SuppressWarnings("unused")
		public void useStored(final StoreTrace storeTrace) {
			// do nothing
		}
	}
}
