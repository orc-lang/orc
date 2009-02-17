package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.runtime.values.Closure;
import orc.trace.events.BlockEvent;
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
import orc.trace.events.SendEvent;
import orc.trace.events.StoreEvent;
import orc.trace.events.UnblockEvent;
import orc.trace.handles.FirstHandle;
import orc.trace.handles.Handle;
import orc.trace.handles.OnlyHandle;
import orc.trace.values.Marshaller;
import orc.trace.values.Value;

/**
 * Implementation for tracers which record events.
 * 
 * @author quark
 */
public abstract class AbstractTracer extends Tracer {
	private class TokenTracerImpl extends TokenTracer {
		/** The current thread */
		private final ForkEvent thread;
		/** The timestamp of the last call made (for {@link ReceiveEvent}). */
		private long lastCallTime = 0;
		/** The current source location (used for all events). */
		private SourceLocation location;
		/** Used to free any StoreEvent after the storing token dies. */
		private StoreEvent store = null;
		
		private TokenTracerImpl(ForkEvent fork, SourceLocation location) {
			this.thread = fork;
			this.location = location;
		}
		
		public PullTrace pull() {
			PullEvent pull = new PullEvent();
			annotateAndRecord(new FirstHandle<Event>(pull));
			return pull;
		}
		public TokenTracerImpl fork() {
			ForkEvent fork = new ForkEvent();
			annotateAndRecord(new FirstHandle<Event>(fork));
			return new TokenTracerImpl(fork, location);
		}
		public void send(Object site, Object[] arguments) {
			// serialize arguments
			Value[] arguments2 = new Value[arguments.length];
			for (int i = 0; i < arguments.length; ++i) {
				arguments2[i] = marshaller.marshal(arguments[i]);
			}
			annotateAndRecord(new FirstHandle<Event>(new SendEvent(marshaller.marshal(site), arguments2)));
			lastCallTime = System.currentTimeMillis();
		}
		public void choke(StoreTrace store) {
			annotateAndRecord(new OnlyHandle<Event>(new ChokeEvent((StoreEvent)store)));
		}
		public void enter(Closure closure) {
			// Do nothing; we need to create an event
		}
		public void leave(int depth) {
			// Do nothing; we need to create an event
		}
		public void receive(Object value) {
			if (lastCallTime == 0) {
				// if a corresponding send event was not called, then
				// don't record any latency
				annotateAndRecord(new OnlyHandle<Event>(new ReceiveEvent(
						marshaller.marshal(value))));
			} else {
				int latency = (int)(System.currentTimeMillis() - lastCallTime);
				annotateAndRecord(new OnlyHandle<Event>(new ReceiveEvent(
						marshaller.marshal(value), latency)));
			}
		}
		public void die() {
			if (store != null) {
				// free any store event; the interface contract
				// guarantees that all references to this event
				// are recorded before the token which produced
				// it dies
				annotateAndRecord(new OnlyHandle<Event>(new FreeEvent(store)));
			}
			annotateAndRecord(new OnlyHandle<Event>(new DieEvent()));
		}
		public void block(PullTrace pull) {
			annotateAndRecord(new OnlyHandle<Event>(new BlockEvent((PullEvent)pull)));
		}
		public StoreTrace store(PullTrace event, Object value) {
			// we'll record a FreeEvent for the store when the token dies
			store = new StoreEvent((PullEvent)event, marshaller.marshal(value));
			annotateAndRecord(new FirstHandle<Event>(store));
			return store;
		}
		public void unblock(StoreTrace store) {
			annotateAndRecord(new OnlyHandle<Event>(new UnblockEvent((StoreEvent)store)));
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

		protected void annotateAndRecord(final Handle<? extends Event> eventh) {
			Event event = eventh.get();
			event.setSourceLocation(location);
			event.setThread(thread);
			record(eventh);
		}

		public void setSourceLocation(SourceLocation location) {
			this.location = location;
		}

		public SourceLocation getSourceLocation() {
			return location;
		}

		public void useStored(StoreTrace storeTrace) {
			// Do nothing: this event is not relevant
			// to debugging traces. It would also be
			// problematic to record because we can't
			// tell when the last useStored occurs for
			// a particular StoreEvent, so we can't mark
			// the last appearance of that StoreEvent in
			// the stream.
		}
	}
	
	/** Marshaller for values. */
	private final Marshaller marshaller;

	public AbstractTracer() {
		marshaller = new Marshaller();
	}
	
	public TokenTracerImpl start() {
		ForkEvent thread = new RootEvent();
		thread.setSourceLocation(SourceLocation.UNKNOWN);
		record(new FirstHandle<Event>(thread));
		return new TokenTracerImpl(thread, SourceLocation.UNKNOWN);
	}
	
	protected abstract void record(Handle<? extends Event> event);
}
