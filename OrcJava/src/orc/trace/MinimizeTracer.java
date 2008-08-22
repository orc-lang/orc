package orc.trace;

import orc.error.SourceLocation;
import orc.trace.events.BeforeEvent;
import orc.trace.events.Event;
import orc.trace.events.PullEvent;
import orc.trace.events.StoreEvent;

/**
 * Wrap a tracer to ignore all but the events essential to reconstruct
 * the trace using the same (deterministic) engine:
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
	public void after(BeforeEvent before) {}

	@Override
	public BeforeEvent before() {
		return null;
	}

	@Override
	public void block(PullEvent pull) {}

	@Override
	public void choke(StoreEvent store) {}

	@Override
	public void print(String value, boolean newline) {}

	@Override
	public void publish(Object value) {}

	@Override
	public PullEvent pull() {
		return null;
	}

	@Override
	public void setSourceLocation(SourceLocation location) {}

	@Override
	public StoreEvent store(PullEvent event, Object value) {
		return null;
	}

	@Override
	public void unblock(StoreEvent store) {}

	@Override
	public void send(Object site, Object[] arguments) {}
}
