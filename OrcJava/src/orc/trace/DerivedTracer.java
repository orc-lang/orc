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
 * Base class for tracers which delegate to something else.
 * Useful to create a tracer which filters (ignores) certain
 * events.
 * @author quark
 */
public abstract class DerivedTracer implements Tracer {
	private Tracer tracer;
	public DerivedTracer(Tracer tracer) {
		this.tracer = tracer;
	}

	public void after(BeforeEvent before) {
		tracer.after(before);
	}

	public BeforeEvent before() {
		return tracer.before();
	}
	
	public void block(PullEvent pull) {
		tracer.block(pull);
	}

	public void choke(StoreEvent store) {
		tracer.choke(store);
	}
	
	public void die() {
		tracer.die();
	}
	
	public void error(TokenException error) {
		tracer.error(error);
	}
	
	public Tracer fork() {
		return tracer.fork();
	}
	
	public void free(Event event) {
		tracer.free(event);
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
	
	public PullEvent pull() {
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
	
	public void start() {
		tracer.start();
	}
	
	public StoreEvent store(PullEvent event, Object value) {
		return tracer.store(event, value);
	}
	
	public void unblock(StoreEvent store) {
		tracer.unblock(store);
	}
}
