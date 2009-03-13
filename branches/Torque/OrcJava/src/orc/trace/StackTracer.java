package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.runtime.nodes.Def;
import orc.runtime.values.Closure;
import orc.trace.TokenTracer.BeforeTrace;
import orc.trace.TokenTracer.PullTrace;
import orc.trace.TokenTracer.StoreTrace;
import orc.trace.events.Event;

/**
 * @author quark
 */
public class StackTracer extends DerivedTracer {
	public StackTracer(Tracer tracer) {
		super(tracer);
	}

	@Override
	protected TokenTracer newTokenTracer(TokenTracer tracer) {
		return new StackTokenTracer(tracer, null);
	}
	
	public class StackTrace {
		public StackTrace parent;
		public Def def;
		public int recursion = 1;
		public StackTrace(Def def, StackTrace parent) {
			this.def = def;
			this.parent = parent;
		}
	}
	
	private class StackTokenTracer extends DerivedTokenTracer {
		public StackTrace stack;
		
		public StackTokenTracer(TokenTracer tracer, StackTrace stack) {
			super(tracer);
			this.stack = stack;
		}
		
		@Override
		public TokenTracer fork() {
			return new StackTokenTracer(tracer.fork(), stack);
		}

		@Override
		public void enter(Closure closure) {
			if (stack != null && stack.def == closure.def) {
				stack.recursion++;
			} else {
				stack = new StackTrace(closure.def, stack);
			}
			super.enter(closure);
		}
		
		@Override
		public void leave(int depth) {
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
