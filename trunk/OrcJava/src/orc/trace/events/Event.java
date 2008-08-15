package orc.trace.events;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import orc.trace.handles.Handle;

public abstract class Event implements Serializable {
	public final Handle<ForkEvent> thread;
	public Event(final Handle<ForkEvent> thread) {
		super();
		this.thread = thread;
	}
	/**
	 * Return a characteristic (but not guaranteed unique) 
	 * human-readable label for the event.
	 */
	public String label() {
		return Integer.toHexString(hashCode());
	}
	public void prettyPrint(Writer out) throws IOException {
		out.write(thread.get().label());
		out.write(":");
		out.write(getClass().getSimpleName());
	}
}
