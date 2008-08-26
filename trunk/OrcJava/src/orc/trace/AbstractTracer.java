package orc.trace;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.trace.events.AfterEvent;
import orc.trace.events.BeforeEvent;
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
public abstract class AbstractTracer implements Tracer {
	private class TokenTracerImpl implements TokenTracer {
		/** The current thread */
		private final ForkEvent thread;
		/** The timestamp of the last call made (for {@link ReceiveEvent}). */
		private long lastCallTime;
		/** The current source location (used for all events). */
		private SourceLocation location;
		
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
		public void receive(Object value) {
			int latency = (int)(System.currentTimeMillis() - lastCallTime);
			annotateAndRecord(new OnlyHandle<Event>(new ReceiveEvent(
					marshaller.marshal(value), latency)));
		}
		public void die() {
			annotateAndRecord(new OnlyHandle<Event>(new DieEvent()));
		}
		public void block(PullTrace pull) {
			annotateAndRecord(new OnlyHandle<Event>(new BlockEvent((PullEvent)pull)));
		}
		public StoreTrace store(PullTrace event, Object value) {
			StoreEvent store = new StoreEvent((PullEvent)event, marshaller.marshal(value));
			annotateAndRecord(new FirstHandle<Event>(store));
			return store;
		}
		public void unblock(StoreTrace store) {
			annotateAndRecord(new OnlyHandle<Event>(new UnblockEvent((StoreEvent)store)));
		}
		public void finishStore(StoreTrace event) {
			annotateAndRecord(new OnlyHandle<Event>(new FreeEvent((StoreEvent)event)));
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
		public BeforeTrace before() {
			BeforeEvent out = new BeforeEvent();
			annotateAndRecord(new FirstHandle<Event>(out));
			return out;
		}
		public void after(BeforeTrace before) {
			annotateAndRecord(new OnlyHandle<Event>(new AfterEvent((BeforeEvent)before)));
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
	}
	
	/** Marshaller for values. */
	private final Marshaller marshaller;

	public AbstractTracer() {
		marshaller = new Marshaller();
	}
	
	public TokenTracerImpl start() {
		ForkEvent thread = new RootEvent();
		// the root event is not annotated with a thread
		thread.setSourceLocation(SourceLocation.UNKNOWN);
		record(new FirstHandle<Event>(thread));
		return new TokenTracerImpl(thread, SourceLocation.UNKNOWN);
	}
	
	protected abstract void record(Handle<? extends Event> event);
}
