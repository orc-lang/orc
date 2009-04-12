package orc.trace.events;

import orc.trace.handles.LastHandle;

/**
 * Always the last event in a thread.
 * @author quark
 */
public class DieEvent extends Event {
	public void setThread(ForkEvent thread) {
		this.thread = new LastHandle<ForkEvent>(thread);
	}
	@Override
	public String getType() { return "die"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
