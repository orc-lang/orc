package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;
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
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		value.prettyPrint(out, indent+1);
		out.write(")");
	}
	public Term getProperty(String key) {
		if (key.equals("value")) return value;
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "publish"; }
}
