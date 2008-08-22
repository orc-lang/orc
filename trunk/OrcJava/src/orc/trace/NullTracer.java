package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.runtime.values.GroupCell;
import orc.trace.events.BeforeEvent;
import orc.trace.events.Event;
import orc.trace.events.PullEvent;
import orc.trace.events.StoreEvent;
import orc.trace.query.predicates.Predicate;

/**
 * Do-nothing tracer, used when tracing is not enabled.
 * @author quark
 */
public final class NullTracer implements Tracer {
	public void setFilter(Predicate filter) {}
	public void send(Object site, Object[] arguments) {}
	public void die() {}
	public void choke(StoreEvent store) {}
	public void receive(Object value) {}
	public void unblock(StoreEvent store) {}
	public Tracer fork() {
		return this;
	}
	public void free(Event event) {}
	public void start() {}
	public void flush() {}
	public void print(String value, boolean newline) {}
	public void publish(Object value) {}
	public void error(TokenException error) {}
	public void setSourceLocation(SourceLocation location) {}
	public SourceLocation getSourceLocation() {
		return null;
	}
	public Tracer fork(GroupCell group) {
		return null;
	}
	public void block(PullEvent pull) {}
	public PullEvent pull() {
		return null;
	}
	public StoreEvent store(PullEvent event, Object value) {
		return null;
	}
	public void after(BeforeEvent before) {
	}
	public BeforeEvent before() {
		return null;
	}
}
