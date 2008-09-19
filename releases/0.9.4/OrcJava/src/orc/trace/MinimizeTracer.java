package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.trace.TokenTracer.BeforeTrace;
import orc.trace.TokenTracer.PullTrace;
import orc.trace.TokenTracer.StoreTrace;
import orc.trace.events.Event;

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
	public MinimizeTracer(Tracer tracer) {
		super(tracer);
	}

	@Override
	protected TokenTracer newTokenTracer(TokenTracer tracer) {
		return new MinimizeTokenTracer(tracer);
	}
	
	private class MinimizeTokenTracer extends DerivedTokenTracer {
		public MinimizeTokenTracer(TokenTracer tracer) {
			super(tracer);
		}

		private boolean inSend = false;
		@Override
		public void after(BeforeTrace before) {}
	
		@Override
		public BeforeTrace before() {
			return null;
		}
	
		@Override
		public void block(PullTrace pull) {}
	
		@Override
		public void choke(StoreTrace store) {}
	
		@Override
		public void print(String value, boolean newline) {}
	
		@Override
		public void publish(Object value) {}
	
		@Override
		public PullTrace pull() {
			return null;
		}
	
		@Override
		public void setSourceLocation(SourceLocation location) {}
	
		@Override
		public StoreTrace store(PullTrace event, Object value) {
			return null;
		}
	
		@Override
		public void unblock(StoreTrace store) {}
	
		@Override
		public void send(Object site, Object[] arguments) {
			inSend = true;
		}
	
		@Override
		public void receive(Object value) {
			super.receive(value);
			inSend = false;
		}
	
		@Override
		public void die() {
			if (inSend) super.die();
		}
	
		@Override
		public void error(TokenException error) {
			if (inSend) super.error(error);
		}

		public void useStored(StoreTrace storeTrace) {
			// do nothing
		}
	}
}
