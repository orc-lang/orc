package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;

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
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "event",
				new ConstantValue(event.get()));
	}
	public Term getProperty(String key) {
		if (key.equals("event")) return event.get();
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "free"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
