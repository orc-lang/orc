package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.trace.TokenTracer.StoreTrace;

/**
 * Base class for tracers which delegate to something else.
 * Useful to create a tracer which filters (ignores) certain
 * events.
 * 
 * @author quark
 */
public abstract class DerivedTracer implements Tracer {
	private Tracer tracer;
	public DerivedTracer(Tracer tracer) {
		this.tracer = tracer;
	}
	
	public TokenTracer start() {
		return newTokenTracer(tracer.start());
	}
	
	public void finish() {
		tracer.finish();
	}
	
	protected abstract TokenTracer newTokenTracer(TokenTracer tracer);
	
	protected abstract class DerivedTokenTracer implements TokenTracer {
		private TokenTracer tracer;
		public DerivedTokenTracer(TokenTracer tracer) {
			this.tracer = tracer;
		}

		public void after(BeforeTrace before) {
			tracer.after(before);
		}

		public BeforeTrace before() {
			return tracer.before();
		}
		
		public void block(PullTrace pull) {
			tracer.block(pull);
		}

		public void choke(StoreTrace store) {
			tracer.choke(store);
		}
		
		public void die() {
			tracer.die();
		}
		
		public void error(TokenException error) {
			tracer.error(error);
		}
		
		public TokenTracer fork() {
			return newTokenTracer(tracer.fork());
		}
		
		public SourceLocation getSourceLocation() {
			return tracer.getSourceLocation();
		}
		
		public void print(String value, boolean newline) {
			tracer.print(value, newline);
		}
		
		public void publish(Object value) {
			tracer.publish(value);
		}
		
		public PullTrace pull() {
			return tracer.pull();
		}
		
		public void receive(Object value) {
			tracer.receive(value);
		}
		
		public void send(Object site, Object[] arguments) {
			tracer.send(site, arguments);
		}
		
		public void setSourceLocation(SourceLocation location) {
			tracer.setSourceLocation(location);
		}
		
		public StoreTrace store(PullTrace event, Object value) {
			return tracer.store(event, value);
		}
		
		public void unblock(StoreTrace store) {
			tracer.unblock(store);
		}
		
		public void useStored(StoreTrace storeTrace) {
			tracer.useStored(storeTrace);
		}
	}
}
