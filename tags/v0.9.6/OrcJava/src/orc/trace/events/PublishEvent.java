package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;
import orc.trace.values.Value;

/**
 * A top-level publication in a thread.
 */
public class PublishEvent extends Event {
	public final Value value;
	public PublishEvent(Value value) {
		this.value = value;
	}
	@Override
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "value", value);
	}
	@Override
	public Term getProperty(String key) {
		if (key.equals("value")) return value;
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "publish"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
