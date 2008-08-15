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
	public void prettyPrint(Writer out) throws IOException {
		out.write(Integer.toHexString(thread.get().hashCode()));
		out.write(":");
		out.write(getClass().getSimpleName());
	}
}
