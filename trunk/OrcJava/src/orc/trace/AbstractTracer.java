package orc.trace;

import orc.error.runtime.TokenException;
import orc.trace.events.BlockEvent;
import orc.trace.events.CallEvent;
import orc.trace.events.DieEvent;
import orc.trace.events.ErrorEvent;
import orc.trace.events.Event;
import orc.trace.events.ForkEvent;
import orc.trace.events.ChokeEvent;
import orc.trace.events.FreeEvent;
import orc.trace.events.PrintEvent;
import orc.trace.events.PublishEvent;
import orc.trace.events.ResumeEvent;
import orc.trace.events.StoreEvent;
import orc.trace.events.UnblockEvent;
import orc.trace.handles.FirstHandle;
import orc.trace.handles.Handle;
import orc.trace.handles.OnlyHandle;
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
	/** Marshaller for values. */
	private final Marshaller marshaller;

	public AbstractTracer() {
		this(ForkEvent.ROOT, new Marshaller());
	}

	/** Copy constructor for use by {@link #forked(ForkEvent, Marshaller)}. */
	protected AbstractTracer(ForkEvent thread, Marshaller marshaller) {
		this.thread = thread;
		this.marshaller = marshaller;
	}
	
	public void start() {
		record(new FirstHandle<Event>(thread));
	}
	public Tracer fork() {
		ForkEvent fork = new ForkEvent(thread);
		record(new FirstHandle<Event>(fork));
		// we can't fork during a site call, so no need
		// to track the caller
		return forked(fork, marshaller);
	}
	public void call(Object site, Object[] arguments) {
		// serialize arguments
		Value[] arguments2 = new Value[arguments.length];
		for (int i = 0; i < arguments.length; ++i) {
			arguments2[i] = marshaller.marshal(arguments[i]);
		}
		record(new OnlyHandle<Event>(new CallEvent(thread,
				marshaller.marshal(site), arguments2)));
	}
	public void choke(StoreEvent store) {
		record(new OnlyHandle<Event>(new ChokeEvent(thread, store)));
	}
	public void resume(Object value) {
		record(new OnlyHandle<Event>(new ResumeEvent(thread, marshaller.marshal(value))));
	}
	public void die() {
		record(new OnlyHandle<Event>(new DieEvent(thread)));
	}
	public void block() {
		record(new OnlyHandle<Event>(new BlockEvent(thread)));
	}
	public StoreEvent store(Object value) {
		StoreEvent store = new StoreEvent(thread, marshaller.marshal(value));
		record(new FirstHandle<Event>(store));
		return store;
	}
	public void unblock(StoreEvent store) {
		record(new OnlyHandle<Event>(new UnblockEvent(thread, store)));
	}
	public void free(Event event) {
		record(new OnlyHandle<Event>(new FreeEvent(thread, event)));
	}
	public void error(TokenException error) {
		record(new OnlyHandle<Event>(new ErrorEvent(thread, error)));
	}
	public void print(String value, boolean newline) {
		record(new OnlyHandle<Event>(new PrintEvent(thread, value, newline)));
	}
	public void publish(Object value) {
		record(new OnlyHandle<Event>(new PublishEvent(thread, marshaller.marshal(value))));
	}
	
	protected abstract void record(Handle<? extends Event> event);
	protected abstract Tracer forked(ForkEvent fork, Marshaller marshaller);
}
