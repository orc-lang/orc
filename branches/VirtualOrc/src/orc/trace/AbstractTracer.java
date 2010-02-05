//
// AbstractTracer.java -- Java class AbstractTracer
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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

		private TokenTracerImpl(final ForkEvent fork, final SourceLocation location) {
			this.thread = fork;
			this.location = location;
		}

		@Override
		public PullTrace pull() {
			final PullEvent pull = new PullEvent();
			annotateAndRecord(new FirstHandle<Event>(pull));
			return pull;
		}

		@Override
		public TokenTracerImpl fork() {
			final ForkEvent fork = new ForkEvent();
			annotateAndRecord(new FirstHandle<Event>(fork));
			return new TokenTracerImpl(fork, location);
		}

		@Override
		public void send(final Object site, final Object[] arguments) {
			// serialize arguments
			final Value[] arguments2 = new Value[arguments.length];
			for (int i = 0; i < arguments.length; ++i) {
				arguments2[i] = marshaller.marshal(arguments[i]);
			}
			annotateAndRecord(new FirstHandle<Event>(new SendEvent(marshaller.marshal(site), arguments2)));
			lastCallTime = System.currentTimeMillis();
		}

		@Override
		public void choke(final StoreTrace store) {
			annotateAndRecord(new OnlyHandle<Event>(new ChokeEvent((StoreEvent) store)));
		}

		@Override
		public void enter(final Closure closure) {
			// Do nothing; we need to create an event
		}

		@Override
		public void leave(final int depth) {
			// Do nothing; we need to create an event
		}

		@Override
		public void receive(final Object value) {
			if (lastCallTime == 0) {
				// if a corresponding send event was not called, then
				// don't record any latency
				annotateAndRecord(new OnlyHandle<Event>(new ReceiveEvent(marshaller.marshal(value))));
			} else {
				final int latency = (int) (System.currentTimeMillis() - lastCallTime);
				annotateAndRecord(new OnlyHandle<Event>(new ReceiveEvent(marshaller.marshal(value), latency)));
			}
		}

		@Override
		public void die() {
			annotateAndRecord(new OnlyHandle<Event>(new DieEvent()));
		}

		@Override
		public void block(final PullTrace pull) {
			annotateAndRecord(new OnlyHandle<Event>(new BlockEvent((PullEvent) pull)));
		}

		@Override
		public StoreTrace store(final PullTrace event, final Object value) {
			final StoreEvent store = new StoreEvent((PullEvent) event, marshaller.marshal(value));
			annotateAndRecord(new FirstHandle<Event>(store));
			return store;
		}

		@Override
		public void unblock(final StoreTrace store) {
			annotateAndRecord(new OnlyHandle<Event>(new UnblockEvent((StoreEvent) store)));
		}

		@Override
		public void error(final TokenException error) {
			annotateAndRecord(new OnlyHandle<Event>(new ErrorEvent(error)));
		}

		@Override
		public void print(final String value, final boolean newline) {
			annotateAndRecord(new OnlyHandle<Event>(new PrintEvent(value, newline)));
		}

		@Override
		public void publish(final Object value) {
			annotateAndRecord(new OnlyHandle<Event>(new PublishEvent(marshaller.marshal(value))));
		}

		protected void annotateAndRecord(final Handle<? extends Event> eventh) {
			final Event event = eventh.get();
			event.setSourceLocation(location);
			event.setThread(thread);
			record(eventh);
		}

		public void setSourceLocation(final SourceLocation location) {
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

	@Override
	public TokenTracerImpl start() {
		final ForkEvent thread = new RootEvent();
		thread.setSourceLocation(SourceLocation.UNKNOWN);
		record(new FirstHandle<Event>(thread));
		return new TokenTracerImpl(thread, SourceLocation.UNKNOWN);
	}

	protected abstract void record(Handle<? extends Event> event);
}
