package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.runtime.values.Closure;

/**
 * Do-nothing tracer, used when tracing is not enabled.
 * @author quark
 */
public class NullTracer extends Tracer {
	public TokenTracer start() {
		return TOKEN_TRACER;
	}
	public void finish() {}
	private static final TokenTracer TOKEN_TRACER = new TokenTracer() {
		public void send(Object site, Object[] arguments) {}
		public void die() {}
		public void choke(StoreTrace store) {}
		public void receive(Object value) {}
		public void unblock(StoreTrace store) {}
		public TokenTracer fork() {
			return this;
		}
		public void enter(Closure closure) {}
		public void leave(int depth) {}
		public void print(String value, boolean newline) {}
		public void publish(Object value) {}
		public void error(TokenException error) {}
		public void setSourceLocation(SourceLocation location) {}
		public SourceLocation getSourceLocation() {
			return null;
		}
		public void block(PullTrace pull) {}
		public PullTrace pull() {
			return null;
		}
		public StoreTrace store(PullTrace event, Object value) {
			return null;
		}
	};
}
