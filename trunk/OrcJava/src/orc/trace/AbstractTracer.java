package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.runtime.values.GroupCell;
import orc.trace.events.AfterEvent;
import orc.trace.events.BeforeEvent;
import orc.trace.events.BlockEvent;
import orc.trace.events.SendEvent;
import orc.trace.events.ChokeEvent;
import orc.trace.events.DieEvent;
import orc.trace.events.ErrorEvent;
import orc.trace.events.Event;
import orc.trace.events.ForkEvent;
import orc.trace.events.FreeEvent;
import orc.trace.events.PrintEvent;
import orc.trace.events.PublishEvent;
import orc.trace.events.PullEvent;
import orc.trace.events.ReceiveEvent;
import orc.trace.events.RootEvent;
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
 * TODO: make which events are written configurable.
 * 
 * @author quark
 */
public abstract class AbstractTracer implements Tracer {
	/** The current thread */
	private final ForkEvent thread;
	/** The last call made (for {@link ReceiveEvent}). */
	private SendEvent call;
	/** The timestamp of the last call made (for {@link ReceiveEvent}). */
	private long lastCallTime;
	/** The current source location (used for all events). */
	private SourceLocation location;
	/** Marshaller for values. */
	private final Marshaller marshaller;

	public AbstractTracer() {
		thread = new RootEvent();
		marshaller = new Marshaller();
		location = SourceLocation.UNKNOWN;
	}

	/** Copy constructor for use by {@link #forked(ForkEvent)}. */
	protected AbstractTracer(AbstractTracer that, ForkEvent fork) {
		// that.call should be null so we don't need to copy it
		this.location = that.location;
		this.marshaller = that.marshaller;
		this.thread = fork;
	}
	
	public void start() {
		// the root event is not annotated with a thread
		thread.setSourceLocation(location);
		record(new FirstHandle<Event>(thread));
	}
	public PullEvent pull() {
		PullEvent pull = new PullEvent();
		annotateAndRecord(new FirstHandle<Event>(pull));
		return pull;
	}
	public Tracer fork() {
		ForkEvent fork = new ForkEvent();
		annotateAndRecord(new FirstHandle<Event>(fork));
		// we can't fork during a site call, so no need
		// to track the caller
		return forked(fork);
	}
	public void send(Object site, Object[] arguments) {
		// serialize arguments
		Value[] arguments2 = new Value[arguments.length];
		for (int i = 0; i < arguments.length; ++i) {
			arguments2[i] = marshaller.marshal(arguments[i]);
		}
		call = new SendEvent(marshaller.marshal(site), arguments2);
		lastCallTime = System.currentTimeMillis();
		annotateAndRecord(new FirstHandle<Event>(call));
	}
	public void choke(StoreEvent store) {
		annotateAndRecord(new OnlyHandle<Event>(new ChokeEvent(store)));
	}
	public void receive(Object value) {
		assert(call != null);
		int latency = (int)(System.currentTimeMillis() - lastCallTime);
		annotateAndRecord(new OnlyHandle<Event>(new ReceiveEvent(
				marshaller.marshal(value), call, latency)));
		call = null;
	}
	public void die() {
		annotateAndRecord(new OnlyHandle<Event>(new DieEvent()));
	}
	public void block(PullEvent pull) {
		annotateAndRecord(new OnlyHandle<Event>(new BlockEvent(pull)));
	}
	public StoreEvent store(PullEvent event, Object value) {
		StoreEvent store = new StoreEvent(event, marshaller.marshal(value));
		annotateAndRecord(new FirstHandle<Event>(store));
		return store;
	}
	public void unblock(StoreEvent store) {
		annotateAndRecord(new OnlyHandle<Event>(new UnblockEvent(store)));
	}
	public void free(Event event) {
		annotateAndRecord(new OnlyHandle<Event>(new FreeEvent(event)));
	}
	public void error(TokenException error) {
		annotateAndRecord(new OnlyHandle<Event>(new ErrorEvent(error)));
	}
	public void print(String value, boolean newline) {
		annotateAndRecord(new OnlyHandle<Event>(new PrintEvent(value, newline)));
	}
	public void publish(Object value) {
		annotateAndRecord(new OnlyHandle<Event>(new PublishEvent(marshaller.marshal(value))));
	}
	public BeforeEvent before() {
		BeforeEvent out = new BeforeEvent();
		annotateAndRecord(new FirstHandle<Event>(out));
		return out;
	}
	public void after(BeforeEvent before) {
		annotateAndRecord(new OnlyHandle<Event>(new AfterEvent(before)));
	}

	protected void annotateAndRecord(final Handle<? extends Event> eventh) {
		Event event = eventh.get();
		event.setSourceLocation(location);
		event.setThread(thread);
		record(eventh);
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
