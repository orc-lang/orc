package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.trace.events.BlockEvent;
import orc.trace.events.CallEvent;
import orc.trace.events.ChokeEvent;
import orc.trace.events.DieEvent;
import orc.trace.events.ErrorEvent;
import orc.trace.events.Event;
import orc.trace.events.ForkEvent;
import orc.trace.events.FreeEvent;
import orc.trace.events.PrintEvent;
import orc.trace.events.PublishEvent;
import orc.trace.events.ResumeEvent;
import orc.trace.events.StoreEvent;
import orc.trace.events.UnblockEvent;
import orc.trace.handles.FirstHandle;
import orc.trace.handles.Handle;
import orc.trace.handles.OnlyHandle;
import orc.trace.query.EventCursor;
import orc.trace.query.Frame;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.Result;
import orc.trace.values.Marshaller;
import orc.trace.values.Value;

/**
 * Base class for tracers.
 * FIXME: make which events are written configurable.
 * 
 * @author quark
 */
public abstract class AbstractTracer implements Tracer {
	/** The current thread */
	private final ForkEvent thread;
	/** The last call made (for {@link ResumeEvent}). */
	private CallEvent call;
	/** The current source location (used for all events). */
	private SourceLocation location;
	/** Marshaller for values. */
	private final Marshaller marshaller;
	/** Events must satisfy this predicate to be traced. */
	protected Predicate filter;

	public AbstractTracer() {
		thread = ForkEvent.ROOT;
		marshaller = new Marshaller();
		filter = null;
		location = SourceLocation.UNKNOWN;
	}
	
	public void setFilter(Predicate filter) {
		this.filter = filter;
	}

	/** Copy constructor for use by {@link #forked(ForkEvent)}. */
	protected AbstractTracer(AbstractTracer that, ForkEvent fork) {
		this.marshaller = that.marshaller;
		this.filter = that.filter;
		this.location = that.location;
		this.thread = fork;
	}
	
	public void start() {
		maybeRecord(new FirstHandle<Event>(thread));
	}
	public Tracer fork() {
		ForkEvent fork = new ForkEvent(thread);
		maybeRecord(new FirstHandle<Event>(fork));
		// we can't fork during a site call, so no need
		// to track the caller
		return forked(fork);
	}
	public void call(Object site, Object[] arguments) {
		// serialize arguments
		Value[] arguments2 = new Value[arguments.length];
		for (int i = 0; i < arguments.length; ++i) {
			arguments2[i] = marshaller.marshal(arguments[i]);
		}
		call = new CallEvent(thread, marshaller.marshal(site), arguments2);
		maybeRecord(new FirstHandle<Event>(call));
	}
	public void choke(StoreEvent store) {
		maybeRecord(new OnlyHandle<Event>(new ChokeEvent(thread, store)));
	}
	public void resume(Object value) {
		assert(call != null);
		maybeRecord(new OnlyHandle<Event>(new ResumeEvent(
				thread, marshaller.marshal(value), call)));
		call = null;
	}
	public void die() {
		maybeRecord(new OnlyHandle<Event>(new DieEvent(thread)));
	}
	public void block() {
		maybeRecord(new OnlyHandle<Event>(new BlockEvent(thread)));
	}
	public StoreEvent store(Object value) {
		StoreEvent store = new StoreEvent(thread, marshaller.marshal(value));
		maybeRecord(new FirstHandle<Event>(store));
		return store;
	}
	public void unblock(StoreEvent store) {
		maybeRecord(new OnlyHandle<Event>(new UnblockEvent(thread, store)));
	}
	public void free(Event event) {
		maybeRecord(new OnlyHandle<Event>(new FreeEvent(thread, event)));
	}
	public void error(TokenException error) {
		maybeRecord(new OnlyHandle<Event>(new ErrorEvent(thread, error)));
	}
	public void print(String value, boolean newline) {
		maybeRecord(new OnlyHandle<Event>(new PrintEvent(thread, value, newline)));
	}
	public void publish(Object value) {
		maybeRecord(new OnlyHandle<Event>(new PublishEvent(thread, marshaller.marshal(value))));
	}
	
	protected void maybeRecord(final Handle<? extends Event> event) {
		if (filter != null) {
			// we'll use a special frame containing only the
			// current event
			// TODO: make this able to scan in the stream
			Frame frame = Frame.newFrame(new EventCursor() {
					public Event current() throws EndOfStream {
						return event.get();
					}
					public EventCursor forward() throws EndOfStream {
						throw new EndOfStream();
					}
					public EventCursor back() throws EndOfStream {
						throw new EndOfStream();
					}
				});
			Result result = filter.evaluate(frame);
			if (result == Result.NO) return;
		}
		event.get().setSourceLocation(location);
		record(event);
	}
	
	protected abstract void record(Handle<? extends Event> event);
	protected abstract Tracer forked(ForkEvent fork);

	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}
