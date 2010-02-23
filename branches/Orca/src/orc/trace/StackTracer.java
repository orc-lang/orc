//
// StackTracer.java -- Java class StackTracer
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

import orc.ast.oil.expression.Def;
import orc.runtime.values.Closure;

/**
 * @author quark
 */
public class StackTracer extends DerivedTracer {
	public StackTracer(final Tracer tracer) {
		super(tracer);
	}

	@Override
	protected TokenTracer newTokenTracer(final TokenTracer tracer) {
		return new StackTokenTracer(tracer, null);
	}

	public class StackTrace {
		public StackTrace parent;
		public Def def;
		public int recursion = 1;

		public StackTrace(final Def def, final StackTrace parent) {
			this.def = def;
			this.parent = parent;
		}
	}

	private class StackTokenTracer extends DerivedTokenTracer {
		public StackTrace stack;

		public StackTokenTracer(final TokenTracer tracer, final StackTrace stack) {
			super(tracer);
			this.stack = stack;
		}

		@Override
		public TokenTracer fork() {
			return new StackTokenTracer(tracer.fork(), stack);
		}

		@Override
		public void enter(final Closure closure) {
			if (stack != null && stack.def == closure.def) {
				stack.recursion++;
			} else {
				stack = new StackTrace(closure.def, stack);
			}
			super.enter(closure);
		}

		@Override
		public void leave(final int depth) {
			for (int i = depth; i > 0;) {
				if (i < stack.recursion) {
					stack.recursion -= i;
					break;
				} else {
					i -= stack.recursion;
					stack = stack.parent;
				}
			}
			super.leave(depth);
		}
	}
}
