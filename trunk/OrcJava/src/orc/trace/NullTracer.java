package orc.trace;

import orc.error.runtime.TokenException;
import orc.trace.events.Event;
import orc.trace.events.StoreEvent;
import orc.trace.query.predicates.Predicate;

/**
 * Do-nothing tracer, used when tracing is not enabled.
 * @author quark
 */
public final class NullTracer implements Tracer {
	public void setFilter(Predicate filter) {}
	public void call(Object site, Object[] arguments) {}
	public void die() {}
	public void choke(StoreEvent store) {}
	public void resume(Object value) {}
	public void block() {}
	public void unblock(StoreEvent store) {}
	public StoreEvent store(Object value) {
		return null;
	}
	public Tracer fork() {
		return this;
	}
	public void free(Event event) {}
	public void start() {}
	public void flush() {}
	public void print(String value, boolean newline) {}
	public void publish(Object value) {}
	public void error(TokenException error) {}
}
