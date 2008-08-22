package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;

/**
 * Dummy event used to free a handle to an event.
 * FIXME: I wish we could get rid of this. It has no
 * semantic value and is only necessary to support
 * correct memory management of handles.
 * 
 * @author quark
 */
public class FreeEvent extends Event {
	@SuppressWarnings("unused")
	private Handle<Event> event;
	public FreeEvent(Event event) {
		this.event = new LastHandle<Event>(event);
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		event.get().prettyPrint(out, indent+1);
		out.write(")");
	}
	public Term getProperty(String key) {
		if (key.equals("event")) return event.get();
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "free"; }
}
