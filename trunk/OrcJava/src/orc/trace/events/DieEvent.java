package orc.trace.events;

import orc.trace.handles.LastHandle;

/**
 * Always the last event in a thread.
 * @author quark
 */
public class DieEvent extends Event {
	public DieEvent(ForkEvent thread) {
		super(new LastHandle<ForkEvent>(thread));
	}
}
